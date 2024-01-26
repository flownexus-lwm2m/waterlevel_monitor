package com.example;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.LeshanServerBuilder;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.send.SendListener;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLink;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointsProvider;

import com.example.json.JacksonLinkSerializer;
import com.example.json.JacksonLwM2mNodeSerializer;
import com.example.json.JacksonRegistrationSerializer;
import com.example.json.JacksonVersionSerializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.example.DataSenderRest;

import java.util.Collection;
import java.nio.ByteBuffer;

public class MyLeshanServer {

    private static LeshanServer server;
    private static DataSenderRest dataSenderRest = new DataSenderRest();
    private static ObjectMapper mapper;
    private static SimpleModule module;

    public static void main(String[] args) {
        LeshanServerBuilder builder = new LeshanServerBuilder();

        builder.setEndpointsProviders(new CaliforniumServerEndpointsProvider());
        server = builder.build();
        server.start();

        server.getRegistrationService().addListener(new MyRegistrationListener());
        server.getObservationService().addListener(new MyObservationListener());
        //server.getSendService().addListener(new MySendListener());

        mapper = new ObjectMapper();
        module = new SimpleModule();
        module.addSerializer(Link.class, new JacksonLinkSerializer());
        //this.module.addSerializer(Registration.class, new JacksonRegistrationSerializer(server.getPresenceService()));
        // TODO like we have a dedicated serializer for Registration, we maybe need one for RegistrationUpdate
        // needed for : registrationListener.updated(RegistrationUpdate, Registration, Registration)
        module.addSerializer(LwM2mNode.class, new JacksonLwM2mNodeSerializer());
        module.addSerializer(Version.class, new JacksonVersionSerializer());
        mapper.registerModule(module);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.stop();
            }
        });

        System.out.println("Leshan server started");
    }

    private static class MyRegistrationListener implements RegistrationListener {

        @Override
        public void registered(Registration registration, Registration previousReg,
                                      Collection<Observation> previousObsersations) {
            System.out.println("new device: " + registration.getEndpoint());

            /* Discover resources on the client*/
            LwM2mLink[] res;
            try {
                DiscoverResponse discoverResponse = server.send(registration, new DiscoverRequest(3));
                if (discoverResponse.isSuccess()) {
                    res = discoverResponse.getObjectLinks();
                    if (res != null) {
                        System.out.println("Resources:");
                        for (LwM2mLink link : res) {
                            System.out.println(link);
                        }
                    } else {
                        System.out.println("No resources found.");
                    }
                } else {
                    System.err.println("Failed to discover resources: " +
                                             discoverResponse.getErrorMessage());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            /* Subscribe to individual objects */
            int[][] objectLinks = {{3303, 0, 5700}, {3304, 0, 5701}, {3305, 0, 5702}};
            for (int[] link : objectLinks) {
                ObserveRequest observeRequest = new ObserveRequest(link[0], link[1], link[2]);
                try {
                    server.send(registration, observeRequest);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void updated(RegistrationUpdate update, Registration updatedReg,
                                  Registration previousReg) {
            System.out.println("Device updated: " + updatedReg.getEndpoint());
        }

        @Override
        public void unregistered(Registration registration, Collection<Observation> observations,
                                         boolean expired,
                                         Registration newReg) {
            System.out.println("Device left: " + registration.getEndpoint());
        }
    }

    private static class MyObservationListener implements ObservationListener {

        @Override
        public void cancelled(Observation observation) {
        }

        @Override
        public void onResponse(SingleObservation observation, Registration registration,
                                      ObserveResponse response) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            //System.out.println("onResponse (single): " + response);
            String jsonContent = null;
            try {
                jsonContent = mapper.writeValueAsString(response.getContent());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            if (registration != null) {
                String data = new StringBuilder("{\"ep\":\"") //
                        .append(registration.getEndpoint()) //
                        .append("\",\"res\":\"") //
                        .append(observation.getPath()).append("\",\"val\":") //
                        .append(jsonContent) //
                        .append("}") //
                        .toString();

                dataSenderRest.sendData(data);
            }
        }

        @Override
        public void onResponse(CompositeObservation observation, Registration registration,
                                      ObserveCompositeResponse response) {
            String jsonContent = null;
            String jsonListOfPath = null;
            try {
                jsonContent = mapper.writeValueAsString(response.getContent());
                List<String> paths = new ArrayList<String>();
                for (LwM2mPath path : response.getObservation().getPaths()) {
                    paths.add(path.toString());
                }
                jsonListOfPath = mapper.writeValueAsString(paths);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            if (registration != null) {
                String data = new StringBuilder("{\"ep\":\"") //
                        .append(registration.getEndpoint()) //
                        .append("\",\"val\":") //
                        .append(jsonContent) //
                        .append(",\"paths\":") //
                        .append(jsonListOfPath) //
                        .append("}") //
                        .toString();

                dataSenderRest.sendData(data);
            }
        }


        @Override
        public void onError(Observation observation, Registration registration, Exception error) {
            System.out.println("onError: " + registration.getEndpoint());
            System.out.println("onError: " + error);
        }

        @Override
        public void newObservation(Observation observation, Registration registration) {
        }
    }

    private static class MySendListener implements SendListener {

        @Override
        public void dataReceived(Registration registration,
                                         TimestampedLwM2mNodes data, SendRequest request) {
            System.out.println("dataReceived from: " + registration.getEndpoint());
            System.out.println("data: " + data);
        }

        @Override
        public void onError(Registration registration,
                                 String errorMessage, Exception error) {
          System.out.println("Unable to handle Send Request from: " + registration.getEndpoint());
          System.out.println(errorMessage);
          System.out.println(error);
        }
    }
}
