package com.ny.redpackets.dao;

import com.ny.redpackets.model.Packet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

/**
 * @author N.Y
 * @date 2019-06-29 16:08
 */
@Repository
public interface PacketDao extends JpaRepository<Packet, String> {
    /**
     * 这个jpa只确保了这个既定类Packet中的属性的增删改查，如果有其他的业务也要，还需要自定义
     */
    /**
     * 通过
     * @param tel
     * @return
     */
    Packet findByTel(String tel);

    /**
     * @Transactional该注解表明此方法有事务处理
     * @Procedure注解表明有存储过程发生
     * CREATE DEFINER=`root`@`localhost` PROCEDURE `bind`(IN i_id varchar(36), IN i_tel varchar(30), OUT o_result int)
     * ......
     * @param id
     * @param tel
     * @return
     */
    @Transactional
    @Procedure(procedureName = "bind", outputParameterName = "o_result")
    Integer bindRedPacket(@Param("i_id")String id, @Param("i_tel")String tel);
}
