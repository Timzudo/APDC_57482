package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.*;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.LoginData;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

	//Logger object
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	private final Gson g = new Gson();
	
	public LoginResource() {
		
	}
	
	/*@POST
	@Path("/v1")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLoginV1(LoginData data){
		LOG.fine("Login attempt by user: " + data.username);

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Entity user = datastore.get(userKey);

		if(user != null){
			String password = user.getString("user_pwd");
			if(password.equals(DigestUtils.sha512Hex(data.password))){
				AuthToken token = new AuthToken(data.username);
				LOG.info("User " + data.username + " logged in sucessfully.");
				return Response.ok(g.toJson(token)).build();
			}
			else{
				LOG.warning("Wrong password for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		else{
			LOG.warning("Failed login attempt for userename: " + data.username);
			return Response.status(Status.FORBIDDEN).build();
		}
	}*/

	/*@POST
	@Path("/v2")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLoginV2(LoginData data, @Context HttpServletRequest request, @Context HttpHeaders headers){
		LOG.fine("Login attempt by user: " + data.username);

		//Keys sao geradas fora das transactions
		Key userKey = datastore.newKeyFactory()
						.setKind("User")
						.newKey(data.username);
		Key ctrsKey = datastore.newKeyFactory()
						.addAncestor(PathElement.of("User", data.username))
						.setKind("UserStats")
						.newKey("counters");

		Key logKey = datastore.allocateId(
						datastore.newKeyFactory()
						.addAncestor(PathElement.of("User", data.username))
						.setKind("UserLog")
						.newKey());

		Transaction txn = datastore.newTransaction();
		try{
			Entity user = txn.get(userKey);
			if(user == null){
				LOG.warning("Failed login attempt for username " + data.username);
				return Response.status(Status.FORBIDDEN).build();
			}

			Entity stats = txn.get(ctrsKey);
			if(stats == null){
				stats = Entity.newBuilder(ctrsKey)
						.set("user_stats_login", 0L)
						.set("user_stats_failed", 0L)
						.set("user_first_login", Timestamp.now())
						.set("user_last_login", Timestamp.now())
						.build();
			}

			String password = user.getString("user_pwd");
			if(password.equals(DigestUtils.sha512Hex(data.password))) {

				Entity log = Entity.newBuilder(logKey)
						.set("user_login_ip", request.getRemoteAddr())
						.set("user_login_host", request.getRemoteHost())
						.set("user_login_latlon",
								StringValue.newBuilder(headers.getHeaderString("X-AppEngine-CityLatLong"))
										.setExcludeFromIndexes(true).build())
						.set("user_login_city", headers.getHeaderString("X-AppEngine-City"))
						.set("user_login_country", headers.getHeaderString("X-AppEngine-Country"))
						.set("user_login_time", Timestamp.now())
						.build();


				//TODO
				Entity ustats = Entity.newBuilder(ctrsKey)
						.set("user_stats_login", 1L + stats.getLong("user_stats_login"))
						.set("user_stats_failed", 0L)
						.set("user_first_login", stats.getTimestamp("user_first_login"))
						.set("user_last_login", Timestamp.now())
						.build();

				txn.put(ustats, log);
				txn.commit();

				AuthToken token = new AuthToken(data.username);
				LOG.info("User " + data.username + " logged in sucessfully.");
				return Response.ok(g.toJson(token)).build();
			}
			else{
				//TODO
				Entity ustats = Entity.newBuilder(ctrsKey)
						.set("user_stats_login", stats.getLong("user_stats_login"))
						.set("user_stats_failed", 1L + stats.getLong("user_stats_failed"))
						.set("user_first_login", stats.getTimestamp("user_first_login"))
						.set("user_last_login", stats.getTimestamp("user_last_login"))
						.set("user_last_attempt", Timestamp.now())
						.build();

				txn.put(ustats);
				txn.commit();

				LOG.warning("Wrong password for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		catch (Exception e){
			txn.rollback();
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
			if(txn.isActive()){
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}*/

	@POST
	@Path("/user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response checkUsernameAvailable(LoginData data){

		//TODO userkeyfactory global
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Entity user = datastore.get(userKey);

		if(user != null && user.getString("user_pwd").equals(DigestUtils.sha512Hex(data.password))){

			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, -1);
			Timestamp yestereday = Timestamp.of(cal.getTime());

			Query<Entity> query = Query.newEntityQueryBuilder()
									.setKind("UserLog")
									.setFilter(
											StructuredQuery.CompositeFilter.and(
													StructuredQuery.PropertyFilter.hasAncestor(userKey),
													StructuredQuery.PropertyFilter.ge("user_login_time", yestereday)
											)
									)
									.setOrderBy(StructuredQuery.OrderBy.desc("user_login_time"))
									.setLimit(3)
									.build();

			QueryResults<Entity> logs = datastore.run(query);

			List<Date> loginDates = new ArrayList<>();
			logs.forEachRemaining(userlog -> {
				loginDates.add(userlog.getTimestamp("user_login_time").toDate());
			});

			LOG.info("Returned logins for user: " + data.username);
			return Response.ok(g.toJson(loginDates)).build();
		}
		else{
			LOG.warning("Wrong password for username: " + data.username);
			return Response.status(Status.FORBIDDEN).build();
		}
	}

	@GET
	@Path("/yau")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response yau(){
		return Response.ok(g.toJson("yeet")).build();
	}
}
