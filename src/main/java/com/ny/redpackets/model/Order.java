package com.ny.redpackets.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author N.Y
 * @date 2019-06-29 10:53
 */

/**
 * 这个实体Order对应的是数据库中的i_order表
 */
@Table(name="i_order")
@Entity
@Getter
@Setter
@ToString
public class Order {
    @Id
    /**
     * 电话
     */
    @Column
    private String tel;
    /**
     * 抢到的红包编号
     */
    @Column(name="packet_id")
    private String packetId;

}
