package com.wimn.devices;

import com.wimn.models.DepartmentType;
import com.wimn.models.DeviceType;
import com.wimn.models.NetworkDevice;

import java.util.ArrayList;
import java.util.List;

public class RouterDevice extends NetworkDevice {
    private String wanIp;
    private boolean natEnabled;
    private List<String> advertisedRoutes;

    public RouterDevice(String id, String name, String wanIp, String lanIp,
                        String defaultGateway, double x, double y, double z) {
        super(id, name, DeviceType.ROUTER, DepartmentType.CORE_INFRA, lanIp, "2001:db8:1::1",
                "00:1A:2B:3C:4D:01", "255.255.255.0", defaultGateway, 1, "Fiber 10Gbps",
                null, x, y, z);
        this.wanIp = wanIp;
        this.natEnabled = true;
        this.advertisedRoutes = new ArrayList<>();
    }

    public String getWanIp() { return wanIp; }
    public void setWanIp(String wanIp) { this.wanIp = wanIp; }

    public boolean isNatEnabled() { return natEnabled; }
    public void setNatEnabled(boolean natEnabled) { this.natEnabled = natEnabled; }

    public List<String> getAdvertisedRoutes() { return advertisedRoutes; }
}
