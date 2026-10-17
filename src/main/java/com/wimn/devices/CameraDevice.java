package com.wimn.devices;

import com.wimn.models.DepartmentType;
import com.wimn.models.DeviceType;
import com.wimn.models.NetworkDevice;

public class CameraDevice extends NetworkDevice {
    private String resolution;
    private int fps;
    private double streamBitrateMbps;
    private boolean isRecording;
    private String zoneCovered;

    public CameraDevice(String id, String name, DepartmentType department, String ip,
                        String mac, String gateway, int vlanId, String connectedSwitchId,
                        String resolution, String zoneCovered, double x, double y, double z) {
        super(id, name, DeviceType.CCTV_CAMERA, department, ip, "2001:db8:1::" + Math.abs(id.hashCode() % 250),
                mac, "255.255.255.0", gateway, vlanId, "Ethernet PoE 1Gbps",
                connectedSwitchId, x, y, z);
        this.resolution = resolution;
        this.fps = 30;
        this.streamBitrateMbps = 4.5;
        this.isRecording = true;
        this.zoneCovered = zoneCovered;
    }

    public String getResolution() { return resolution; }
    public int getFps() { return fps; }
    public double getStreamBitrateMbps() { return streamBitrateMbps; }
    public boolean isRecording() { return isRecording; }
    public void setRecording(boolean recording) { isRecording = recording; }
    public String getZoneCovered() { return zoneCovered; }
}
