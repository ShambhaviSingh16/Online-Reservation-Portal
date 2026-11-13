package com.trainticket.api;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApiValidationTest {
    @Test void registrationAndBookingRejectInvalidFields() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var validator=factory.getValidator();
            assertFalse(validator.validate(new AuthApi.Registration("x","short","bad"," ")).isEmpty());
            assertFalse(validator.validate(new ReservationApi.Booking(-1,null,false)).isEmpty());
            assertFalse(validator.validate(new ReservationApi.GroupBookingRequest(1,java.util.UUID.randomUUID(),java.util.List.of(" "),false)).isEmpty());
            assertFalse(validator.validate(new ReservationApi.GroupBookingRequest(1,java.util.UUID.randomUUID(),java.util.Collections.nCopies(7,"Passenger"),false)).isEmpty());
            assertTrue(validator.validate(new ReservationApi.GroupBookingRequest(1,java.util.UUID.randomUUID(),java.util.List.of("Asha Rao","Ravi Shah"),false)).isEmpty());
            assertTrue(validator.validate(new AuthApi.Registration("newuser","LongPassword123!","new@example.com","New User")).isEmpty());
        }
    }
}
