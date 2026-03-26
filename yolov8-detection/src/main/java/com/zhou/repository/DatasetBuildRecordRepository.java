package com.zhou.repository;

import com.zhou.model.DatasetBuildRecordEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DatasetBuildRecordRepository {

    @Insert("""
            INSERT INTO dataset_build_records (record_id, dataset_name, dataset_path, output_preset,
                                               train_count, val_count, test_count, val_ratio, test_ratio, created_at)
            VALUES (#{recordId}, #{datasetName}, #{datasetPath}, #{outputPreset},
                    #{trainCount}, #{valCount}, #{testCount}, #{valRatio}, #{testRatio}, #{createdAt})
            """)
    int save(DatasetBuildRecordEntity entity);
}

