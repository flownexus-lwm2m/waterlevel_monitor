package com.example;

import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.LeshanServerBuilder;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.send.SendListener;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLink;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointsProvider;

import java.util.Collection;

public class MyLeshanServer {

	private static LeshanServer server;

	public static void main(String[] args) {
		LeshanServerBuilder builder = new LeshanServerBuilder();
		
		builder.setEndpointsProviders(new CaliforniumServerEndpointsProvider());
		server = builder.build();
		server.start();
		
		server.getRegistrationService().addListener(new MyRegistrationListener());
		server.getObservationService().addListener(new MyObservationListener());
		server.getSendService().addListener(new MySendListener());
		
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
			System.out.println("Discover Target: " + registration.getRootPath());

			/* Discover resources on the client*/
			LwM2mLink[] res;
			try {
				DiscoverResponse discoverResponse = server.send(registration, new DiscoverRequest(3));
				if (discoverResponse.isSuccess()) {
					res = discoverResponse.getObjectLinks();
					if (res != null) {
						StringBuilder sb = new StringBuilder();
						for (LwM2mLink link : res) {
							sb.append(link).append('\n');
							System.out.println("Link: " + link);
						}
						System.out.println("Resources:\n" + sb.toString());
					} else {
						System.out.println("No resources found.");
					}
				} else {
					System.err.println("Failed to discover resources: " + discoverResponse.getErrorMessage());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

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
			System.out.println("onResponse: " + registration.getEndpoint());
			System.out.println("onResponse: " + response);
		}

		@Override
		public void onResponse(CompositeObservation observation, Registration registration,
									  ObserveCompositeResponse response) {
			System.out.println("onResponse: " + registration.getEndpoint());
			System.out.println("onResponse: " + response);
		}


		@Override
		public void onError(Observation observation, Registration registration, Exception error) {
			System.out.println("onError: " + registration.getEndpoint());
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
