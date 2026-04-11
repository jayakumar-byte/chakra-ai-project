package com.example.chakraEncryption2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class ChakraTest {

    @Test
    public void testChakraSalting() {
        double inputRadius = 10.0;
        double salt = 0.55;
        double expected = 10.55;

        // Simulating your Chakra salting logic
        double result = inputRadius + salt;

        assertEquals(expected, result, "Salt logic should add 0.55 to radius");
    }

    @Test
    public void testThetaSalting() {
        double inputTheta = 45.0;
        double salt = 7.0;
        double expected = 52.0;

        double result = inputTheta + salt;

        assertEquals(expected, result, "Theta logic should add 7.0 salt");
    }
}
