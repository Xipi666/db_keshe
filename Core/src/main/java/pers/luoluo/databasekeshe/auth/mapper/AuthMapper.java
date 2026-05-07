package pers.luoluo.databasekeshe.auth.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import pers.luoluo.databasekeshe.auth.domain.SysUser;

@Mapper
public interface AuthMapper {

    @Select("""
            SELECT ID, USERNAME, PASSWORD_HASH, DISPLAY_NAME, ROLE_CODE, PERSON_ID, STATUS
            FROM SYS_USER
            WHERE USERNAME = #{username}
            """)
    SysUser findByUsername(@Param("username") String username);

    @Insert("""
            INSERT INTO SYS_USER (
                ID,
                USERNAME,
                PASSWORD_HASH,
                DISPLAY_NAME,
                ROLE_CODE,
                PERSON_ID,
                STATUS,
                LAST_LOGIN_AT,
                CREATED_AT,
                UPDATED_AT
            )
            VALUES (
                SEQ_SYS_USER.NEXTVAL,
                #{username},
                #{passwordHash},
                #{displayName},
                #{roleCode},
                NULL,
                0,
                NULL,
                SYSTIMESTAMP,
                NULL
            )
            """)
    int insertUser(
            @Param("username") String username,
            @Param("passwordHash") String passwordHash,
            @Param("displayName") String displayName,
            @Param("roleCode") String roleCode
    );

    @Update("""
            UPDATE SYS_USER
            SET LAST_LOGIN_AT = SYSTIMESTAMP,
                UPDATED_AT = SYSTIMESTAMP
            WHERE ID = #{id}
            """)
    int updateLastLoginAt(@Param("id") Long id);
}
