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
 * @date 2019-06-29 16:02
 */
@Table(name="i_packet")
@Entity
@Getter
@Setter
@ToString
public class Packet {

    @Id
    /**
     * 红包id
     */
    @Column
    private String id;
    /**
     * 红包名称
     */
    @Column
    private String name;
    /**
     * 红包金额
     */
    @Column
    private Integer value;
    /**
     * 绑定的手机号码
     */
    @Column
    private String tel;

}
