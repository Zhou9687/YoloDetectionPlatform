package com.zhou.repository;

import com.zhou.model.PredictionRecordEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PredictionRecordRepository {

    @Insert("""
            INSERT INTO prediction_records (result_id, file_name, relative_path, result_image_path,
                                            model_path, conf, iou, device, imgsz, augment, max_det, boxes_json, created_at)
            VALUES (#{resultId}, #{fileName}, #{relativePath}, #{resultImagePath},
                    #{modelPath}, #{conf}, #{iou}, #{device}, #{imgsz}, #{augment}, #{maxDet}, #{boxesJson}, #{createdAt})
            """)
    int save(PredictionRecordEntity entity);
}

