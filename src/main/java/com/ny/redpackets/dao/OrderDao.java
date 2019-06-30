package com.ny.redpackets.dao;

import com.ny.redpackets.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author N.Y
 * @date 2019-06-29 16:06
 */

/**
 * @Repository标识此类为dao层的接入接口
 */
@Repository
public interface OrderDao extends JpaRepository<Order, String> {
}
