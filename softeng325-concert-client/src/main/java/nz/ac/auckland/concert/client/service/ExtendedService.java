package nz.ac.auckland.concert.client.service;

import nz.ac.auckland.concert.common.dto.ConcertDTO;
import nz.ac.auckland.concert.common.dto.PerformerDTO;
import nz.ac.auckland.concert.common.message.Messages;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ExtendedService extends DefaultService {

    // TODO: error handling for these boys
    public ExtendedService() {
        _client = Config.POOLED_CLIENT;
    }

    public PerformerDTO createPerformer(PerformerDTO performerDTO) {
        try {
            Response res = _client
                    .target(Config.LOCAL_SERVER_ADDRESS + "/resources/performers")
                    .request()
                    .header("Authorization", _authorizationToken) // Insert authorisation token
                    .accept(MediaType.APPLICATION_XML)
                    .post(Entity.xml(performerDTO));

            return res.readEntity(PerformerDTO.class);

        } catch (ServiceUnavailableException | ProcessingException e) {
            e.printStackTrace();
            throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
        }
    }

    public ConcertDTO createConcert(ConcertDTO concertDTO) {
        try {
            Response res = _client
                    .target(Config.LOCAL_SERVER_ADDRESS + "/resources/concerts")
                    .request()
                    .header("Authorization", _authorizationToken) // Insert authorisation token
                    .accept(MediaType.APPLICATION_XML)
                    .post(Entity.xml(concertDTO));

            return res.readEntity(ConcertDTO.class);

        } catch (ServiceUnavailableException | ProcessingException e) {
            e.printStackTrace();
            throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
        }
    }

    public PerformerDTO addImage(PerformerDTO performerDTO) {
        try {
            Response res = _client
                    .target(Config.LOCAL_SERVER_ADDRESS + "/resources/images")
                    .request()
                    .header("Authorization", _authorizationToken) // Insert authorisation token
                    .accept(MediaType.APPLICATION_XML)
                    .put(Entity.xml(performerDTO));

            return res.readEntity(PerformerDTO.class);

        } catch (ServiceUnavailableException | ProcessingException e) {
            e.printStackTrace();
            throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
        }
    }


    public String subscribeToNewPerformers() {
        Response res = _client
                .target(Config.LOCAL_SERVER_ADDRESS + "/resources/performers/getNotifications")
                .request()
                .header("Authorization", _authorizationToken) // Insert authorisation token
                .accept(MediaType.APPLICATION_XML)
                .get();

        return res.readEntity(String.class);
    }

    public String subscribeToNewConcerts() {
        Response res = _client
                .target(Config.LOCAL_SERVER_ADDRESS + "/resources/concerts/getNotifications")
                .request()
                .header("Authorization", _authorizationToken) // Insert authorisation token
                .accept(MediaType.APPLICATION_XML)
                .get();

        return res.readEntity(String.class);
    }

    public String subscribeToNewImages() {
        Response res = _client
                .target(Config.LOCAL_SERVER_ADDRESS + "/resources/images/getNotifications/")
                .request()
                .header("Authorization", _authorizationToken) // Insert authorisation token
                .accept(MediaType.APPLICATION_XML)
                .get();

        return res.readEntity(String.class);
    }

    public String subscribetoNewImagesForPerformer(PerformerDTO performerDTO) {
        Response res = _client
                .target(Config.LOCAL_SERVER_ADDRESS + "/resources/images/getNotifications/" + performerDTO.getId())
                .request()
                .header("Authorization", _authorizationToken) // Insert authorisation token
                .accept(MediaType.APPLICATION_XML)
                .get();

        return res.readEntity(String.class);
    }

}
