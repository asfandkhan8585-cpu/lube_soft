package com.lubesoft.model;

public class LicenseInfo {
    private int id;
    private String machineId;
    private String licenseKey;
    private boolean activated;
    private String installDate;
    private String activatedAt;

    public LicenseInfo() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }

    public String getLicenseKey() { return licenseKey; }
    public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }

    public boolean isActivated() { return activated; }
    public void setActivated(boolean activated) { this.activated = activated; }

    public String getInstallDate() { return installDate; }
    public void setInstallDate(String installDate) { this.installDate = installDate; }

    public String getActivatedAt() { return activatedAt; }
    public void setActivatedAt(String activatedAt) { this.activatedAt = activatedAt; }
}
