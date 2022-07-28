package com.sy599.game.db.bean.redBagRain;

import java.util.Date;

/**
 * creatBy butao
 * date: 2021/5/20 0020
 * desc:
 */
public class GroupRedBagResult {
    private long id;

    private long redbagnum;

    private long redBagConfigId;

    private long groupid;

    private Date time;

    private long userid;

    private String username;

    private Date createtime;

    private Date modifytime;

    public int getUserTakeState() {
        return userTakeState;
    }

    public void setUserTakeState(int userTakeState) {
        this.userTakeState = userTakeState;
    }

    private int userTakeState;
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRedbagnum() {
        return redbagnum;
    }

    public void setRedbagnum(long redbagnum) {
        this.redbagnum = redbagnum;
    }

    public long getRedBagConfigId() {
        return redBagConfigId;
    }

    public void setRedBagConfigId(long redBagConfigId) {
        this.redBagConfigId = redBagConfigId;
    }

    public long getGroupid() {
        return groupid;
    }

    public void setGroupid(long groupid) {
        this.groupid = groupid;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
        this.userid = userid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    public Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    public Date getModifytime() {
        return modifytime;
    }

    public void setModifytime(Date modifytime) {
        this.modifytime = modifytime;
    }

    @Override
    public String toString() {
        return "GroupRedBagResult{" +
                "id=" + id +
                ", redbagnum=" + redbagnum +
                ", redBagConfigId=" + redBagConfigId +
                ", groupid=" + groupid +
                ", time=" + time +
                ", userid=" + userid +
                ", username='" + username + '\'' +
                ", createtime=" + createtime +
                ", modifytime=" + modifytime +
                ", userTakeState=" + userTakeState +
                '}';
    }
}
