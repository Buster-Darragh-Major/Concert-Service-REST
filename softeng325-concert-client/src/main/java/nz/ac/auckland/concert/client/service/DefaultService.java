package nz.ac.auckland.concert.client.service;

import nz.ac.auckland.concert.common.dto.*;
import nz.ac.auckland.concert.common.message.Messages;

import javax.imageio.ImageIO;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Class implementing functionality outlined by the ConcertService interface. All methods
 * here follow the general steps:
 *  1)  Send message to service and receive response
 *  2)  Diagnose any errors and throw appropriate exceptions
 *  3)  Read any embedded entities and return from methods
 */
public class DefaultService implements ConcertService {

    // Constants:
    protected static final int RETRIEVE_WINDOW_SIZE = 10;

    // Fields
    protected Client _client;
    protected String _authorizationToken;
    protected String _username;
    protected String _password;


    public DefaultService() {

        _client = Config.POOLED_CLIENT;

    }


    @Override
    public Set<ConcertDTO> getConcerts() throws ServiceException {

        // Use path parameters to get ranges of results
        int resultListLength = RETRIEVE_WINDOW_SIZE;
        String url = Config.LOCAL_SERVER_ADDRESS + String.format("/concerts?start=%d&size=%d", 0, RETRIEVE_WINDOW_SIZE);

        Set<ConcertDTO> concerts = new HashSet<>();

        while (resultListLength == RETRIEVE_WINDOW_SIZE) { // While still receiving full window size sets
            try {
                Response res = _client.target(url).request().get();

                Set<ConcertDTO> resultList = res.readEntity(new GenericType<Set<ConcertDTO>>() {});
                resultListLength = resultList.size(); // Set as current size of result list, used in while predicate
                url = res.getLocation().toString();

                concerts.addAll(resultList);
            } catch (ServiceUnavailableException | ProcessingException e) {
                throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
            }
        }

        return concerts;
    }

    @Override
    public Set<PerformerDTO> getPerformers() throws ServiceException {

        // Use path parameters to get ranges of results
        int resultListLength = RETRIEVE_WINDOW_SIZE;
        String url = Config.LOCAL_SERVER_ADDRESS + String.format("/performers?start=%d&size=%d", 0, RETRIEVE_WINDOW_SIZE);

        Set<PerformerDTO> performers = new HashSet<>();

        while (resultListLength == RETRIEVE_WINDOW_SIZE) { // While still receiving full window size sets
            try {
                Response res = _client.target(url).request().get();

                Set<PerformerDTO> resultList = res.readEntity(new GenericType<Set<PerformerDTO>>() {});
                resultListLength = resultList.size(); // Set as current size of result list, used in while predicate
                url = res.getLocation().toString();

                performers.addAll(resultList);
            } catch (ServiceUnavailableException | ProcessingException e) {
                throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
            }
        }

        return performers;
    }

    @Override
    public UserDTO createUser(UserDTO newUser) throws ServiceException {

        try {
            Response res = _client.target(Config.LOCAL_SERVER_ADDRESS + "/users").request()
                    .accept(MediaType.APPLICATION_XML).post(Entity.xml(newUser));

            switch(res.getStatus()) {
                case 400: throw new ServiceException(res.readEntity(String.class)); // Incomplete fields
                case 409: throw new ServiceException(res.readEntity(String.class)); // Username conflict
            }

            // Store auth. details
            _authorizationToken = res.getHeaderString("Authorization");

            res = _client
                    .target(res.getLocation())
                    .request()
                    .header("Authorization", _authorizationToken) // Insert authorisation token
                    .get();

            return res.readEntity(UserDTO.class);
        } catch (ServiceUnavailableException | ProcessingException e) {
            throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
        }
    }

    @Override
    public UserDTO authenticateUser(UserDTO user) throws ServiceException {

        try {
            Response res = _client.target(Config.LOCAL_SERVER_ADDRESS + "/users/login").request()
                    .accept(MediaType.APPLICATION_XML).post(Entity.xml(user));

            switch(res.getStatus()) {
                case 400: throw new ServiceException(res.readEntity(String.class)); // Missing username and/or password
                case 401: throw new ServiceException(res.readEntity(String.class)); // Wrong password
                case 404: throw new ServiceException(res.readEntity(String.class)); // No such user exists
            }

            // Store auth. details
            _authorizationToken = res.getHeaderString("Authorization");
            _username = user.getUsername();
            _password = user.getPassword();

            return res.readEntity(UserDTO.class);
        } catch (ServiceUnavailableException | ProcessingException e) {
            throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
        }
    }

    @Override
    public Image getImageForPerformer(PerformerDTO performer) throws ServiceException {

        try {
            Response res = _client.target(Config.LOCAL_SERVER_ADDRESS + "/images/" + performer.getImageName()).request()
                    .accept("image/png").get();

            switch(res.getStatus()) {
                case 404: throw new ServiceException(res.readEntity(String.class));
                case 503: throw new ServiceException(res.readEntity(String.class));
            }

            byte[] byteArray = res.readEntity(byte[].class);
            return ImageIO.read(new ByteArrayInputStream(byteArray));
        } catch (IOException e) { // Couldn't read file
            throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
        } finally {

        }
    }

    @Override
    public ReservationDTO reserveSeats(ReservationRequestDTO reservationRequest) throws ServiceException {

        try {
            Response res = _client
                    .target(Config.LOCAL_SERVER_ADDRESS + "/reserve")
                    .request()
                    .header("Authorization", _authorizationToken) // Insert authorisation token
                    .accept(MediaType.APPLICATION_XML)
                    .post(Entity.xml(reservationRequest));

            switch(res.getStatus()) {
                case 400: throw new ServiceException(res.readEntity(String.class));
                case 401: throw new ServiceException(res.readEntity(String.class));
                case 403: throw new ServiceException(res.readEntity(String.class));
                case 404: throw new ServiceException(res.readEntity(String.class));
                case 409: throw new ServiceException(res.readEntity(String.class));

            }

            return res.readEntity(ReservationDTO.class);
        } catch (ServiceUnavailableException | ProcessingException e) {
            throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
        }
    }

    @Override
    public void confirmReservation(ReservationDTO reservation) throws ServiceException {

        try {
            Response res = _client
                    .target(Config.LOCAL_SERVER_ADDRESS + "/reserve/book")
                    .request()
                    .header("Authorization", _authorizationToken) // Insert authorisation token
                    .accept(MediaType.APPLICATION_XML)
                    .post(Entity.xml(reservation));

            switch (res.getStatus()) {
                case 401: throw new ServiceException(res.readEntity(String.class));
                case 402: throw new ServiceException(res.readEntity(String.class));
                case 403: throw new ServiceException(res.readEntity(String.class));
                case 408: throw new ServiceException(res.readEntity(String.class));
            }
        } catch (ServiceUnavailableException | ProcessingException e) {
            throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
        }
    }

    @Override
    public void registerCreditCard(CreditCardDTO creditCard) throws ServiceException {

        try {
            Response res = _client
                    .target(Config.LOCAL_SERVER_ADDRESS + "/users/payment")
                    .request()
                    .header("Authorization", _authorizationToken) // Insert authorisation token
                    .accept(MediaType.APPLICATION_XML)
                    .post(Entity.xml(creditCard));

            switch (res.getStatus()) {
                case 401: throw new ServiceException(res.readEntity(String.class));
                case 403: throw new ServiceException(res.readEntity(String.class));
            }
        } catch (ServiceUnavailableException | ProcessingException e) {
            throw new ServiceException((Messages.SERVICE_COMMUNICATION_ERROR));
        }
    }

    @Override
    public Set<BookingDTO> getBookings() throws ServiceException {

        // Use path parameters to get ranges of results
        int resultListLength = RETRIEVE_WINDOW_SIZE;
        String url = Config.LOCAL_SERVER_ADDRESS + String.format("/users/book?start=%d&size=%d", 0, RETRIEVE_WINDOW_SIZE);

        Set<BookingDTO> bookings = new HashSet<>();

        while (resultListLength == RETRIEVE_WINDOW_SIZE) { // While still receiving full window size sets
            try {
                Response res = _client
                        .target(url)
                        .request()
                        .header("Authorization", _authorizationToken) // Insert authorization token
                        .get();

                switch (res.getStatus()) {
                    case 401: throw new ServiceException(res.readEntity(String.class));
                    case 403: throw new ServiceException(res.readEntity(String.class));
                }

                Set<BookingDTO> resultList = res.readEntity(new GenericType<Set<BookingDTO>>() {});
                resultListLength = resultList.size(); // Set as current size of result list, used in while predicate
                url = res.getLocation().toString();

                bookings.addAll(resultList);
            } catch (ServiceUnavailableException | ProcessingException e) {
                throw new ServiceException((Messages.SERVICE_COMMUNICATION_ERROR));
            }
        }

        return bookings;
    }
}
