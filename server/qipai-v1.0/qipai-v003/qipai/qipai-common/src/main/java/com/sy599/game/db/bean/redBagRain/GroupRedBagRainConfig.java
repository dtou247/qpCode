package com.sy599.game.db.bean.redBagRain;

import java.util.Date;

/**
 * creatBy butao
 * date: 2021/5/22 0022
 * desc:
 */
public class GroupRedBagRainConfig {
    private long keyid;

    private long groupid;

    private Date pushstarttime;

    private Date pushendtime;

    private int totalpoint;

    private int redbagnum;

    private int redminpoint;

    private int redmaxpoint;

    private int state;

    private String gametypelimit;

    private Date createtime;

    private Date modifytime;

    public GroupRedBagRainConfig() {
    }


    public long getKeyid() {
        return keyid;
    }

    public void setKeyid(long keyid) {
        this.keyid = keyid;
    }

    public long getGroupid() {
        return groupid;
    }

    public void setGroupid(long groupid) {
        this.groupid = groupid;
    }

    public Date getPushstarttime() {
        return pushstarttime;
    }

    public void setPushstarttime(Date pushstarttime) {
        this.pushstarttime = pushstarttime;
    }

    public Date getPushendtime() {
        return pushendtime;
    }

    public void setPushendtime(Date pushendtime) {
        this.pushendtime = pushendtime;
    }

    public int getTotalpoint() {
        return totalpoint;
    }

    public void setTotalpoint(int totalpoint) {
        this.totalpoint = totalpoint;
    }

    public int getRedbagnum() {
        return redbagnum;
    }

    public void setRedbagnum(int redbagnum) {
        this.redbagnum = redbagnum;
    }

    public int getRedminpoint() {
        return redminpoint;
    }

    public void setRedminpoint(int redminpoint) {
        this.redminpoint = redminpoint;
    }

    public int getRedmaxpoint() {
        return redmaxpoint;
    }

    public void setRedmaxpoint(int redmaxpoint) {
        this.redmaxpoint = redmaxpoint;
    }

    public String getGametypelimit() {
        return gametypelimit;
    }

    public void setGametypelimit(String gametypelimit) {
        this.gametypelimit = gametypelimit == null ? null : gametypelimit.trim();
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

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "GroupRedBagRainConfig{" +
                "keyid=" + keyid +
                ", groupid=" + groupid +
                ", pushstarttime=" + pushstarttime +
                ", pushendtime=" + pushendtime +
                ", totalpoint=" + totalpoint +
                ", redbagnum=" + redbagnum +
                ", redminpoint=" + redminpoint +
                ", redmaxpoint=" + redmaxpoint +
                ", state=" + state +
                ", gametypelimit='" + gametypelimit + '\'' +
                ", createtime=" + createtime +
                ", modifytime=" + modifytime +
                '}';
    }
}
