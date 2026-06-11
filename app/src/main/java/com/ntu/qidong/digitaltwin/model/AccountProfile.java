package com.ntu.qidong.digitaltwin.model;

/**
 * 账号档案数据模型
 * 对应后端 AccountProfileDto
 */
public class AccountProfile {
    // 账号信息（必填）
    private long userId = -1;
    private String userName = "";
    private String password = "";  // 注意：存储时应过滤此字段

    // 学生档案（选填）
    private String no = null;           // 学号
    private String name = null;         // 姓名
    private String idNumber = null;     // 身份证号
    private Integer gender = null;      // 性别：0=男，1=女
    private Integer ethnicGroup = null; // 民族：0~55
    private String nativePlace = null;  // 籍贯
    private String birthday = null;     // 生日 (ISO 8601)
    private Integer weight = null;      // 体重 (公斤)
    private Double height = null;       // 身高 (米)

    // 状态标识
    private boolean hasProfile = false;

    // 构造函数
    public AccountProfile() {}

    public AccountProfile(long userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    // Getter & Setter 方法

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }

    public Integer getEthnicGroup() { return ethnicGroup; }
    public void setEthnicGroup(Integer ethnicGroup) { this.ethnicGroup = ethnicGroup; }

    public String getNativePlace() { return nativePlace; }
    public void setNativePlace(String nativePlace) { this.nativePlace = nativePlace; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }

    public boolean isHasProfile() { return hasProfile; }
    public void setHasProfile(boolean hasProfile) { this.hasProfile = hasProfile; }

    /**
     * 获取性别显示文本
     */
    public String getGenderText() {
        if (gender == null) return "未填写";
        switch (gender) {
            case 0: return "男";
            case 1: return "女";
            default: return "未知";
        }
    }

    /**
     * 获取民族显示文本
     */
    public String getEthnicGroupText() {
        if (ethnicGroup == null) return "未填写";
        // 常见民族映射表（0~55）
        String[] ethnicNames = {
                "汉族", "蒙古族", "回族", "藏族", "维吾尔族",
                "苗族", "彝族", "壮族", "布依族", "朝鲜族",
                "满族", "侗族", "瑶族", "白族", "土家族",
                "哈尼族", "哈萨克族", "傣族", "黎族", "傈僳族",
                "佤族", "畲族", "高山族", "拉祜族", "水族",
                "东乡族", "纳西族", "景颇族", "柯尔克孜族", "土族",
                "达斡尔族", "仫佬族", "羌族", "布朗族", "撒拉族",
                "毛南族", "仡佬族", "锡伯族", "阿昌族", "普米族",
                "塔吉克族", "怒族", "乌孜别克族", "俄罗斯族", "鄂温克族",
                "德昂族", "保安族", "裕固族", "京族", "塔塔尔族",
                "独龙族", "鄂伦春族", "赫哲族", "门巴族", "珞巴族",
                "基诺族"
        };
        if (ethnicGroup >= 0 && ethnicGroup < ethnicNames.length) {
            return ethnicNames[ethnicGroup];
        }
        return "其他";
    }

    /**
     * 格式化生日显示
     */
    public String getBirthdayDisplay() {
        if (birthday == null || birthday.isEmpty()) return "未填写";
        try {
            // ISO 8601格式: 2005-01-15T00:00:00 -> 2005-01-15
            return birthday.substring(0, 10);
        } catch (Exception e) {
            return birthday;
        }
    }

    /**
     * 检查是否有学生档案信息
     */
    public boolean hasStudentProfile() {
        return no != null || name != null || idNumber != null ||
               gender != null || ethnicGroup != null || nativePlace != null ||
               birthday != null || weight != null || height != null;
    }

    @Override
    public String toString() {
        return "AccountProfile{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", name='" + name + '\'' +
                ", no='" + no + '\'' +
                ", hasProfile=" + hasProfile +
                '}';
    }
}
