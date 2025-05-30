package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.xmlbeans.impl.xb.xmlconfig.Extensionconfig;

import java.util.Map;

@Mapper
public interface UserMapper {
    /**
     * 根据openid查询用户
     *
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 新增用户
     *
     * @param user
     */
    void insert(User user);

    /**
     * 根据用户id查询用户信息
     *
     * @param id
     * @return
     */
    @Select("select * from user where id = #{id}")
    User getById(Long id);

    /**
     * 统计新增用户
     *
     * @param map
     * @return
     */
    Integer countByMap(Map<String, Object> map);
}
