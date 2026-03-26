package com.zhou.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhou.config.YoloProperties;
import com.zhou.model.TrainingJob;
import com.zhou.model.TrainingJobEntity;
import com.zhou.model.TrainingJobStatus;
import com.zhou.repository.TrainingJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

@Service
public class TrainingService {
    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);
    private final YoloScriptService yoloScriptService;
    private final ObjectMapper objectMapper;
    private final TrainingJobRepository trainingJobRepository;
    private final Path jobsStorePath;
    private final Path datasetsRoot;
    private final Map<String, TrainingJob> jobs = new ConcurrentHashMap<>();
    private final TaskExecutor taskExecutor;
    private final boolean importLegacyJson;
    private final List<Path> datasetSearchRoots;

    public TrainingService(
            YoloScriptService yoloScriptService,
            StorageService storageService,
            ObjectMapper objectMapper,
            TaskExecutor taskExecutor,
            TrainingJobRepository trainingJobRepository,
            YoloProperties yoloProperties,
            @Value("${yolo.training.migration.import-legacy-json:true}") boolean importLegacyJson
    ) {
        this.yoloScriptService = yoloScriptService;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.trainingJobRepository = trainingJobRepository;
        this.jobsStorePath = storageService.workspace().resolve("training-jobs.json").toAbsolutePath().normalize();
        this.datasetsRoot = storageService.datasetsDir().toAbsolutePath().normalize();
        this.importLegacyJson = importLegacyJson;
        this.datasetSearchRoots = yoloProperties.getDatasetSearchRoots().stream()
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .map(Path::of)
                .map(path -> path.isAbsolute() ? path : path.toAbsolutePath())
                .map(Path::normalize)
                .collect(Collectors.toList());
        importLegacyJobsIfNeeded();
        loadJobsFromDatabase();
    }

    public TrainingJob start(String datasetPath, int epochs, int batch, String modelPath, Double conf) {
        String jobId = UUID.randomUUID().toString();
        TrainingJob job = new TrainingJob();
        job.setJobId(jobId);
        job.setDatasetPath(datasetPath);
        job.setModelPath(modelPath);
        job.setEpochs(epochs);
        job.setBatch(batch);
        job.setConf(conf);
        job.setProgress(0);
        job.setCreatedAt(Instant.now());
        job.setStatus(TrainingJobStatus.QUEUED);
        jobs.put(jobId, job);
        safePersistJob(job);
        try {
            taskExecutor.execute(() -> executeTraining(jobId));
        } catch (RejectedExecutionException ex) {
            job.setStatus(TrainingJobStatus.FAILED);
            job.setProgress(100);
            job.setFinishedAt(Instant.now());
            job.setMessage("训练任务队列繁忙，请稍后重试");
            safePersistJob(job);
            log.error("Failed to enqueue training job: {}", jobId, ex);
        }
        return job;
    }

    public TrainingJob get(String jobId) {
        TrainingJob cached = jobs.get(jobId);
        if (cached != null) {
            return cached;
        }
        TrainingJobEntity entity;
        try {
            entity = trainingJobRepository.findById(jobId);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("任务状态查询失败：数据库不可用，请稍后重试", ex);
        }
        if (entity == null) {
            throw new NoSuchElementException("Job not found: " + jobId);
        }
        return toModel(entity);
    }

    // Run in background thread via TaskExecutor so /start can return jobId immediately.
    void executeTraining(String jobId) {
        TrainingJob job = get(jobId);
        job.setStatus(TrainingJobStatus.RUNNING);
        job.setProgress(10);
        safePersistJob(job);

        Path dataset = resolveDatasetPath(job.getDatasetPath());
        if (!Files.exists(dataset)) {
            job.setStatus(TrainingJobStatus.FAILED);
            job.setMessage(buildDatasetNotFoundMessage(job.getDatasetPath()));
            job.setProgress(100);
            job.setFinishedAt(Instant.now());
            safePersistJob(job);
            return;
        }

        try {
            Map<String, Object> trainResult = yoloScriptService.train(
                    dataset,
                    job.getEpochs(),
                    job.getBatch(),
                    job.getModelPath(),
                    job.getConf(),
                    progress -> updateRunningProgress(job, progress)
            );
            job.setStatus(TrainingJobStatus.SUCCESS);
            job.setProgress(100);
            job.setMessage(String.valueOf(trainResult.getOrDefault("status", "ok")));
            Object best = trainResult.get("bestModelPath");
            if (best != null) {
                job.setBestModelPath(String.valueOf(best));
            }
        } catch (Exception ex) {
            job.setStatus(TrainingJobStatus.FAILED);
            job.setProgress(100);
            job.setMessage(ex.getMessage());
        }
        job.setFinishedAt(Instant.now());
        safePersistJob(job);
    }

    private void updateRunningProgress(TrainingJob job, int progress) {
        if (job.getStatus() != TrainingJobStatus.RUNNING) {
            return;
        }
        int mapped = Math.max(10, Math.min(99, progress));
        if (mapped <= job.getProgress()) {
            return;
        }
        job.setProgress(mapped);
        safePersistJob(job);
    }

    private void importLegacyJobsIfNeeded() {
        if (!importLegacyJson || !Files.exists(jobsStorePath)) {
            return;
        }
        try {
            if (trainingJobRepository.count() > 0) {
                return;
            }
        } catch (RuntimeException ex) {
            log.warn("Skip legacy jobs import because database is unavailable", ex);
            return;
        }
        try {
            List<TrainingJob> stored = objectMapper.readValue(jobsStorePath.toFile(), new TypeReference<>() {
            });
            for (TrainingJob job : stored) {
                if (job.getJobId() == null || job.getJobId().isBlank()) {
                    continue;
                }
                jobs.put(job.getJobId(), job);
                try {
                    trainingJobRepository.save(toEntity(job));
                } catch (RuntimeException ignored) {
                    // Skip malformed legacy rows so startup is not blocked.
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void loadJobsFromDatabase() {
        List<TrainingJob> loaded;
        try {
            loaded = trainingJobRepository.findAll().stream()
                    .map(this::toModel)
                    .sorted(Comparator.comparing(TrainingJob::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("Failed to load training jobs from database, continue with in-memory cache", ex);
            return;
        }
        for (TrainingJob job : loaded) {
            jobs.put(job.getJobId(), job);
        }
    }

    private void persistJob(TrainingJob job) {
        jobs.put(job.getJobId(), job);
        trainingJobRepository.save(toEntity(job));
    }

    private void safePersistJob(TrainingJob job) {
        try {
            persistJob(job);
        } catch (RuntimeException ex) {
            // Keep in-memory state available even when DB is temporarily unavailable.
            jobs.put(job.getJobId(), job);
            log.warn("Failed to persist training job {}, fallback to memory only", job.getJobId(), ex);
        }
    }

    private TrainingJobEntity toEntity(TrainingJob job) {
        TrainingJobEntity entity = new TrainingJobEntity();
        entity.setJobId(job.getJobId());
        entity.setDatasetPath(job.getDatasetPath());
        entity.setModelPath(job.getModelPath());
        entity.setEpochs(job.getEpochs());
        entity.setBatch(job.getBatch());
        entity.setConf(job.getConf());
        entity.setProgress(job.getProgress());
        entity.setStatus(job.getStatus() == null ? TrainingJobStatus.QUEUED.name() : job.getStatus().name());
        entity.setCreatedAt(job.getCreatedAt() == null ? Instant.now() : job.getCreatedAt());
        entity.setFinishedAt(job.getFinishedAt());
        entity.setMessage(job.getMessage());
        entity.setBestModelPath(job.getBestModelPath());
        return entity;
    }

    private TrainingJob toModel(TrainingJobEntity entity) {
        TrainingJob job = new TrainingJob();
        job.setJobId(entity.getJobId());
        job.setDatasetPath(entity.getDatasetPath());
        job.setModelPath(entity.getModelPath());
        job.setEpochs(entity.getEpochs());
        job.setBatch(entity.getBatch());
        job.setConf(entity.getConf());
        job.setProgress(entity.getProgress());
        job.setStatus(parseStatus(entity.getStatus()));
        job.setCreatedAt(entity.getCreatedAt());
        job.setFinishedAt(entity.getFinishedAt());
        job.setMessage(entity.getMessage());
        job.setBestModelPath(entity.getBestModelPath());
        return job;
    }

    private TrainingJobStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return TrainingJobStatus.QUEUED;
        }
        try {
            return TrainingJobStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            return TrainingJobStatus.QUEUED;
        }
    }

    private Path resolveDatasetPath(String raw) {
        if (raw == null || raw.isBlank()) {
            return Path.of("").toAbsolutePath().normalize();
        }
        Path input = Path.of(raw);
        if (input.isAbsolute()) {
            return input.normalize();
        }

        List<Path> candidates = new ArrayList<>();
        candidates.add(input.toAbsolutePath().normalize());
        candidates.add(datasetsRoot.resolve(input).normalize());
        for (Path root : datasetSearchRoots) {
            candidates.add(root.resolve(input).normalize());
        }

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    private String buildDatasetNotFoundMessage(String raw) {
        List<String> candidates = new ArrayList<>();
        if (raw != null && !raw.isBlank()) {
            Path input = Path.of(raw);
            if (input.isAbsolute()) {
                candidates.add(input.normalize().toString());
            } else {
                candidates.add(input.toAbsolutePath().normalize().toString());
                candidates.add(datasetsRoot.resolve(input).normalize().toString());
                for (Path root : datasetSearchRoots) {
                    candidates.add(root.resolve(input).normalize().toString());
                }
            }
        }
        String tips = "请在前端选择包含 data.yaml 的数据集目录，或填写绝对路径。";
        if (candidates.isEmpty()) {
            return "Dataset path not found. " + tips;
        }
        return "Dataset path not found. tried=" + String.join(" | ", candidates) + ". " + tips;
    }
}
