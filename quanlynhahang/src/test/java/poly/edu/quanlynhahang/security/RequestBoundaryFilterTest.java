package poly.edu.quanlynhahang.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import tools.jackson.databind.ObjectMapper;
import poly.edu.quanlynhahang.config.ApiErrorWriter;

class RequestBoundaryFilterTest {

    private final RequestBoundaryFilter filter = new RequestBoundaryFilter(new ApiErrorWriter(new ObjectMapper()));

    @Test
    void rejectsOversizedJsonBeforeItReachesControllers() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/reservations");
        request.setContentType("application/json");
        request.setContent(new byte[1_048_577]);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(413, response.getStatus());
    }

    @Test
    void rejectsUnsupportedBodyFormat() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/reservations");
        request.setContentType("text/xml");
        request.setContent("<reservation/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(415, response.getStatus());
    }

    @Test
    void allowsBoundedJsonPayload() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/reservations");
        request.setContentType("application/json");
        request.setContent("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void rejectsOversizedQueryForApiEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.setQueryString("q=" + "a".repeat(4_095));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(414, response.getStatus());
    }
}
