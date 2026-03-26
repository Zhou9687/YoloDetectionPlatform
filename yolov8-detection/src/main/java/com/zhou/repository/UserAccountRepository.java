package com.zhou.repository;

import com.zhou.model.UserAccountEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserAccountRepository {

    @Select("""
            SELECT user_id, username, password_hash, created_at
            FROM users
            WHERE username = #{username}
            LIMIT 1
            """)
    UserAccountEntity findByUsername(String username);

    @Insert("""
            INSERT INTO users (user_id, username, password_hash, created_at)
            VALUES (#{userId}, #{username}, #{passwordHash}, #{createdAt})
            """)
    int save(UserAccountEntity entity);

    @Update("""
            UPDATE users
            SET username = #{newUsername}
            WHERE user_id = #{userId}
            """)
    int updateUsername(@Param("userId") String userId, @Param("newUsername") String newUsername);

    @Update("""
            UPDATE users
            SET password_hash = #{passwordHash}
            WHERE user_id = #{userId}
            """)
    int updatePasswordHash(@Param("userId") String userId, @Param("passwordHash") String passwordHash);
}
