package com.sy.sanguo.game.bean.group;

import java.util.Date;

public class SysGroupLevelConfig {

    private static final long serialVersionUID = 1L;

    Long keyId;
    Integer level;
    String name;
    Long exp;
    Long totalExp;
    Long creditExpLimit;
    Long playExp;
    Integer memberCount;
    String bgList;
    String tbList;
    Date createdTime;
    Date lastUpTime;

    public Long getKeyId() {
        return keyId;
    }

    public void setKeyId(Long keyId) {
        this.keyId = keyId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getExp() {
        return exp;
    }

    public void setExp(Long exp) {
        this.exp = exp;
    }

    public Long getTotalExp() {
        return totalExp;
    }

    public void setTotalExp(Long totalExp) {
        this.totalExp = totalExp;
    }

    public Long getCreditExpLimit() {
        return creditExpLimit;
    }

    public void setCreditExpLimit(Long creditExpLimit) {
        this.creditExpLimit = creditExpLimit;
    }

    public Long getPlayExp() {
        return playExp;
    }

    public void setPlayExp(Long playExp) {
        this.playExp = playExp;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public String getBgList() {
        return bgList;
    }

    public void setBgList(String bgList) {
        this.bgList = bgList;
    }

    public String getTbList() {
        return tbList;
    }

    public void setTbList(String tbList) {
        this.tbList = tbList;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getLastUpTime() {
        return lastUpTime;
    }

    public void setLastUpTime(Date lastUpTime) {
        this.lastUpTime = lastUpTime;
    }
}
