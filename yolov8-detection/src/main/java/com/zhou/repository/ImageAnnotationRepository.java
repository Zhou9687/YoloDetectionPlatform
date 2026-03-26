package com.zhou.repository;

import com.zhou.model.ImageAnnotationEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ImageAnnotationRepository {

    @Select("""
            SELECT image_id, batch_id, relative_path, original_file_name, stored_path, label_path,
                   annotated, uploaded_at, boxes_json
            FROM image_annotations
            WHERE image_id = #{imageId}
            """)
    ImageAnnotationEntity findById(String imageId);

    @Select("""
            SELECT image_id, batch_id, relative_path, original_file_name, stored_path, label_path,
                   annotated, uploaded_at, boxes_json
            FROM image_annotations
            ORDER BY uploaded_at ASC
            """)
    List<ImageAnnotationEntity> findAll();

    @Select("""
            SELECT image_id, batch_id, relative_path, original_file_name, stored_path, label_path,
                   annotated, uploaded_at, boxes_json
            FROM image_annotations
            WHERE batch_id = #{batchId}
            ORDER BY uploaded_at ASC
            """)
    List<ImageAnnotationEntity> findByBatchId(String batchId);

    @Insert("""
            INSERT INTO image_annotations (image_id, batch_id, relative_path, original_file_name, stored_path,
                                           label_path, annotated, uploaded_at, boxes_json)
            VALUES (#{imageId}, #{batchId}, #{relativePath}, #{originalFileName}, #{storedPath},
                    #{labelPath}, #{annotated}, #{uploadedAt}, #{boxesJson})
            ON DUPLICATE KEY UPDATE
                batch_id = VALUES(batch_id),
                relative_path = VALUES(relative_path),
                original_file_name = VALUES(original_file_name),
                stored_path = VALUES(stored_path),
                label_path = VALUES(label_path),
                annotated = VALUES(annotated),
                uploaded_at = VALUES(uploaded_at),
                boxes_json = VALUES(boxes_json)
            """)
    int save(ImageAnnotationEntity entity);

    @Delete("DELETE FROM image_annotations WHERE image_id = #{imageId}")
    int deleteById(String imageId);

    @Delete("DELETE FROM image_annotations")
    int deleteAll();
}

