package com.zhou.repository;

import com.zhou.model.TrainingJobEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TrainingJobRepository {

    @Select("""
            SELECT job_id, dataset_path, model_path, epochs, batch_size AS batch, conf,
                   progress, status, created_at, finished_at, message, best_model_path,
                   precision_score AS `precision`, recall_score AS recall,
                   map50_score AS map50, map95_score AS map95
            FROM training_jobs
            WHERE job_id = #{jobId}
            """)
    TrainingJobEntity findById(String jobId);

    @Select("""
            SELECT job_id, dataset_path, model_path, epochs, batch_size AS batch, conf,
                   progress, status, created_at, finished_at, message, best_model_path,
                   precision_score AS `precision`, recall_score AS recall,
                   map50_score AS map50, map95_score AS map95
            FROM training_jobs
            ORDER BY created_at ASC
            """)
    List<TrainingJobEntity> findAll();

    @Select("SELECT COUNT(1) FROM training_jobs")
    long count();

    @Insert("""
            INSERT INTO training_jobs (job_id, dataset_path, model_path, epochs, batch_size, conf,
                                       progress, status, created_at, finished_at, message, best_model_path,
                                       precision_score, recall_score, map50_score, map95_score)
            VALUES (#{jobId}, #{datasetPath}, #{modelPath}, #{epochs}, #{batch}, #{conf},
                    #{progress}, #{status}, #{createdAt}, #{finishedAt}, #{message}, #{bestModelPath},
                    #{precision}, #{recall}, #{map50}, #{map95})
            ON DUPLICATE KEY UPDATE
                dataset_path = VALUES(dataset_path),
                model_path = VALUES(model_path),
                epochs = VALUES(epochs),
                batch_size = VALUES(batch_size),
                conf = VALUES(conf),
                progress = VALUES(progress),
                status = VALUES(status),
                created_at = VALUES(created_at),
                finished_at = VALUES(finished_at),
                message = VALUES(message),
                best_model_path = VALUES(best_model_path),
                precision_score = VALUES(precision_score),
                recall_score = VALUES(recall_score),
                map50_score = VALUES(map50_score),
                map95_score = VALUES(map95_score)
            """)
    int save(TrainingJobEntity entity);
}
