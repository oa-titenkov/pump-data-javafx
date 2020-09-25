package me.titenkov.model;

public class Spool {

    private String stanNumber;
    private String brigade;
    private String shift;
    private String personalNumber;
    private String materialCode;
    private String ordinalShift;
    private String fusionNumber;
    private String parentFlangeNumber;
    private String childFlangeNumber;
    private String diameter;
    private String length;
    private String weight;
    private String ordinalDraw;
    private String quality;
    private String orderCode;
    private String date;
    private String time;
    private String spoolNumber;

    public String getSpoolNumber() {
        return spoolNumber;
    }

    public Integer getIntegerSpoolNumber() {
        return Integer.valueOf(spoolNumber);
    }

    public Integer getIntegerStanNumber() {
        return Integer.valueOf(stanNumber);
    }

    public void setSpoolNumber(String spoolNumber) {
        this.spoolNumber = spoolNumber;
    }

    public String getStanNumber() {
        return stanNumber;
    }

    public void setStanNumber(String stanNumber) {
        this.stanNumber = stanNumber;
    }

    public String getBrigade() {
        return brigade;
    }

    public void setBrigade(String brigade) {
        this.brigade = brigade;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getPersonalNumber() {
        return personalNumber;
    }

    public void setPersonalNumber(String personalNumber) {
        this.personalNumber = personalNumber;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getOrdinalShift() {
        return ordinalShift;
    }

    public void setOrdinalShift(String ordinalShift) {
        this.ordinalShift = ordinalShift;
    }

    public String getFusionNumber() {
        return fusionNumber;
    }

    public void setFusionNumber(String fusionNumber) {
        this.fusionNumber = fusionNumber;
    }

    public String getParentFlangeNumber() {
        return parentFlangeNumber;
    }

    public void setParentFlangeNumber(String parentFlangeNumber) {
        this.parentFlangeNumber = parentFlangeNumber;
    }

    public String getChildFlangeNumber() {
        return childFlangeNumber;
    }

    public void setChildFlangeNumber(String childFlangeNumber) {
        this.childFlangeNumber = childFlangeNumber;
    }

    public String getDiameter() {
        return diameter;
    }

    public void setDiameter(String diameter) {
        this.diameter = diameter;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getOrdinalDraw() {
        return ordinalDraw;
    }

    public void setOrdinalDraw(String ordinalDraw) {
        this.ordinalDraw = ordinalDraw;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "Стан: " + stanNumber  +
                ", Код: " + materialCode  +
                ", Дата: " + date  +
                ", Время: " + time  +
                ", Номер: " + spoolNumber;
    }
}
