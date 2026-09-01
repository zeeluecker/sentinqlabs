package com.sentinq.checkout.ucp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UcpDiscoveryProfile {

    private Ucp ucp;

    public Ucp getUcp() {
        return ucp;
    }

    public void setUcp(Ucp ucp) {
        this.ucp = ucp;
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ucp {

        private Map<String, List<Service>> services;

        private Map<String, List<Capability>> capabilities;


        public Map<String, List<Service>> getServices() {
            return services;
        }

        public void setServices(
                Map<String, List<Service>> services
        ) {
            this.services = services;
        }


        public Map<String, List<Capability>> getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(
                Map<String, List<Capability>> capabilities
        ) {
            this.capabilities = capabilities;
        }
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Service {

        private String transport;

        private String endpoint;


        public String getTransport() {
            return transport;
        }

        public void setTransport(String transport) {
            this.transport = transport;
        }


        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Capability {

        private String version;


        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
