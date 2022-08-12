package org.exoplatform.mfa.rest.otp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.mfa.api.otp.OtpService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/otp")
@Tag(name = "/otp", description = "Manage otp operations")
public class OtpRestService implements ResourceContainer {
  
  private static final Log LOG = ExoLogger.getLogger(OtpRestService.class);

  private OtpService otpService;

  public OtpRestService(OtpService otpService) {
    this.otpService=otpService;
  }


  @Path("/checkRegistration")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
      summary = "Check if user have activated his OTP",
      method = "GET",
      description = "Check if user have activated his OTP"
  )
  @ApiResponses(
      value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error"), }
  )
  public Response checkRegistration(@Context HttpServletRequest request) {

    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    return Response.ok().entity("{\"result\":\"" + otpService.isMfaInitializedForUser(userId) + "\"}").build();

  }

  @Path("/generateSecret")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
      summary = "Generate New secret OTP for user",
      method = "GET",
      description = "Generate New secret OTP for user"
  )
  @ApiResponses(
      value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error"), }
  )
  public Response generateSecret(@Context HttpServletRequest request) {

    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    if (!otpService.isMfaInitializedForUser(userId)) {
      String secret=otpService.generateSecret(userId);
      String urlFromSecret= otpService.generateUrlFromSecret(userId,secret);
      return Response.ok().entity("{\"secret\":\"" + secret + "\",\"url\":\""+urlFromSecret+"\"}").build();
    } else {
      return Response.ok().build();
    }

  }

  
  @Path("/verify")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
      summary = "Verify OTP token",
      method = "GET",
      description = "Verify OTP token"
  )
  @ApiResponses(
      value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error"), }
  )
  public Response verifyToken(@Context HttpServletRequest request,
                              @Parameter(description = "Token to verify", required = true) @QueryParam("token") String token) {
  
    String userId=null;
    try {

      userId = ConversationState.getCurrent().getIdentity().getUserId();

      boolean otpResult = otpService.validateToken(userId,token);
      request.getSession().setAttribute("mfaValidated",otpResult);
      return Response.ok().entity("{\"result\":\"" + otpResult + "\"}").build();
    } catch (Exception e) {
      LOG.warn("Error when checking OTP token for user='{}', token='{}'", userId, token, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }
}
