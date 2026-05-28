package com.ntu.qidong.digitaltwin.model;

/**
 * 应用访问记录模型
 * 用于 SQLite app_visit 表的存储
 */
public class AppVisit {
    private long id;
    private String visitDate;       // 访问日期 (格式: yyyy-MM-dd)
    private int visitCount;         // 访问次数
    private long lastVisitTime;     // 最后访问时间戳

    public AppVisit() {
    }

    public AppVisit(String visitDate, int visitCount, long lastVisitTime) {
        this.visitDate = visitDate;
        this.visitCount = visitCount;
        this.lastVisitTime = lastVisitTime;
    }

    // Getter 方法
    public long getId() {
        return id;
    }

    public String getVisitDate() {
        return visitDate;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public long getLastVisitTime() {
        return lastVisitTime;
    }

    // Setter 方法
    public void setId(long id) {
        this.id = id;
    }

    public void setVisitDate(String visitDate) {
        this.visitDate = visitDate;
    }

    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
    }

    public void setLastVisitTime(long lastVisitTime) {
        this.lastVisitTime = lastVisitTime;
    }

    @Override
    public String toString() {
        return "AppVisit{" +
                "id=" + id +
                ", visitDate='" + visitDate + '\'' +
                ", visitCount=" + visitCount +
                ", lastVisitTime=" + lastVisitTime +
                '}';
    }
}
