package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.appengine.repackaged.org.apache.commons.codec.digest.DigestUtils;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.cloud.storage.Acl;
import com.google.gson.Gson;
import org.apache.http.client.entity.EntityBuilder;
import pt.unl.fct.di.adc.firstwebapp.util.*;
import sun.misc.REException;


@Path("/ti")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class TrabalhoIndividual {
	
	private static final Logger LOG = Logger.getLogger(TrabalhoIndividual.class.getName());
	
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();
	
	public TrabalhoIndividual() {
		
	}


	@POST
	@Path("/create")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doRegistrationV2(RegistrationData data) {
		LOG.fine("Attempt to register user: " + data.username);

		if(!data.validRegistration()) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}

		Transaction txn = datastore.newTransaction();

		try{
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity user = datastore.get(userKey);

			if(user != null){
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("User already exists.").build();
			}
			else{
				user = Entity.newBuilder(userKey)
						.set("user_name", data.name)
						.set("user_pwd", DigestUtils.sha512Hex(data.password))
						.set("user_email", data.email)
						.set("user_profile", data.profile)
						.set("user_phone", data.phone)
						.set("user_cellPhone", data.cellPhone)
						.set("user_address", data.address)
						.set("user_addressC", data.addressC)
						.set("user_cp", data.cp)
						.set("user_nif", data.nif)
						.set("user_creation_time", Timestamp.now())
						.set("user_role", "USER")
						.set("user_state", "INACTIVE")
						.build();
			}

			txn.add(user);
			LOG.info("User registered " + data.username);
			txn.commit();

			return Response.ok("User created successfully").build();
		}
		finally {
			if(txn.isActive()){
				txn.rollback();
			}
		}

	}

	@POST
	@Path("/remove")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response deleteUser(DeleteData data){
		LOG.fine("Attempt to delete user: " + data.usernameDelete);

		if(!data.isValid()) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}

		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.tokenId);
		Entity token = datastore.get(tokenKey);

		if(token == null){
				return Response.status(Status.FORBIDDEN).entity("Not logged in.").build();
		}

		long interval = System.currentTimeMillis();

		interval -= token.getLong("token_expiration");

		Key userDeleteKey = datastore.newKeyFactory().setKind("User").newKey(data.usernameDelete);
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(token.getString("token_username"));

		Transaction txn = datastore.newTransaction();


		try{

			Entity user = txn.get(userKey);
			int roleUser = Roles.getValue(user.getString("user_role"));
			Entity userDel = txn.get(userDeleteKey);
			int roleDeleteUser = Roles.getValue(userDel.getString("user_role"));

			if(roleUser>roleDeleteUser){
				if(interval < 0){
					txn.delete(userDeleteKey);
					LOG.info("User deleted " + data.usernameDelete);
					txn.commit();
					return Response.ok("User deleted successfully").build();
				}
				else{
					txn.delete(tokenKey);
					txn.commit();
					return Response.status(Status.FORBIDDEN).entity("Session expired.").build();
				}
			}
			else{
				LOG.warning("Role is not high enough.");
				return Response.status(Status.FORBIDDEN).build();
			}


		}
		finally {
			if(txn.isActive()){
				txn.rollback();
			}
		}
	}

	@POST
	@Path("/changepass")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response changePassword(ChangePassword data){

		if(!data.validPassword()){
			return Response.status(Status.BAD_REQUEST).entity("Passwords do not match").build();
		}

		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.tokenId);
		Entity token = datastore.get(tokenKey);

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(token.getString("token_username"));
		Entity user = datastore.get(userKey);

		String oldPassword = DigestUtils.sha512Hex(data.oldPassword);

		if(!oldPassword.equals(user.getString("user_pwd"))){
			return Response.status(Status.FORBIDDEN).build();
		}
		Transaction txn = datastore.newTransaction();

		try{
			Entity newUser = Entity.newBuilder(userKey)
					.set("user_name", user.getString("user_name"))
					.set("user_pwd", DigestUtils.sha512Hex(data.newPassword))
					.set("user_email", user.getString("user_email"))
					.set("user_profile", user.getString("user_profile"))
					.set("user_phone", user.getString("user_phone"))
					.set("user_cellPhone", user.getString("user_cellPhone"))
					.set("user_address", user.getString("user_address"))
					.set("user_addressC", user.getString("user_addressC"))
					.set("user_cp", user.getString("user_cp"))
					.set("user_nif", user.getString("user_nif"))
					.set("user_creation_time", user.getTimestamp("user_creation_time"))
					.set("user_role", user.getString("user_role"))
					.set("user_state", user.getString("user_state"))
					.build();
			txn.put(newUser);
			txn.commit();
			return Response.ok("Password updated successfully!").build();
		}
		finally {
			if(txn.isActive()){
				txn.rollback();
			}
		}
	}

	@POST
	@Path("/token")
	public Response showToken(LogoutData data){
		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.tokenId);
		Entity token = datastore.get(tokenKey);

		return Response.ok(g.toJson(token)).build();
	}

	@POST
	@Path("/modify")
	public Response modifyUser(ModifyData data){
		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.tokenId);
		Entity token = datastore.get(tokenKey);

		if(token == null){
			return Response.status(Status.FORBIDDEN).entity("Not logged in.").build();
		}

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(token.getString("token_username"));
		Entity user = datastore.get(userKey);

		Key newUserKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Entity newUser = datastore.get(newUserKey);

		if(newUser == null){
			return Response.status(Status.BAD_REQUEST).entity("No such user.").build();
		}

		int role = Roles.getValue(user.getString("user_role"));

		Transaction txn = datastore.newTransaction();

		try{
			switch (role){
				case 1:
					if(!data.username.equals(token.getString("token_username"))){
						return Response.status(Status.FORBIDDEN).entity("No permission.").build();
					}
					newUser = Entity.newBuilder(newUserKey)
							.set("user_name", user.getString("user_name"))
							.set("user_pwd", user.getString("user_pwd"))
							.set("user_email", user.getString("user_email"))
							.set("user_profile", data.profile)
							.set("user_phone", data.phone)
							.set("user_cellPhone", data.cellPhone)
							.set("user_address", data.address)
							.set("user_addressC", data.addressC)
							.set("user_cp", data.cp)
							.set("user_nif", data.nif)
							.set("user_creation_time", user.getTimestamp("user_creation_time"))
							.set("user_role", user.getString("user_role"))
							.set("user_state", data.state)
							.build();
					break;

				case 2:
				case 3:
				case 4:
					if(!data.username.equals(token.getString("token_username")) && !(role>Roles.getValue(newUser.getString("user_role")))){
						return Response.status(Status.FORBIDDEN).entity("No permission.").build();
					}
					newUser = Entity.newBuilder(newUserKey)
							.set("user_name", data.name)
							.set("user_pwd", user.getString("user_pwd"))
							.set("user_email", data.email)
							.set("user_profile", data.profile)
							.set("user_phone", data.phone)
							.set("user_cellPhone", data.cellPhone)
							.set("user_address", data.address)
							.set("user_addressC", data.addressC)
							.set("user_cp", data.cp)
							.set("user_nif", data.nif)
							.set("user_creation_time", user.getTimestamp("user_creation_time"))
							.set("user_role", data.role)
							.set("user_state", data.state)
							.build();
					break;
			}
			txn.put(newUser);
			txn.commit();
			return Response.ok("User modified successfully!").build();
		}
		finally {
			if(txn.isActive()){
				txn.rollback();
			}
		}
	}


	@POST
	@Path("/list")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response listUsers(LogoutData data){

		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.tokenId);
		Entity token = datastore.get(tokenKey);

		if(token == null){
			return Response.status(Status.FORBIDDEN).entity("Not logged in.").build();
		}

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(token.getString("token_username"));
		Entity user = datastore.get(userKey);

		int role = Roles.getValue(user.getString("user_role"));


		Query<Entity> query;
		QueryResults<Entity> users;
		switch (role){
			case 1:
				query = Query.newEntityQueryBuilder()
						.setKind("User")
						.setFilter(
								StructuredQuery.CompositeFilter.and(
										StructuredQuery.PropertyFilter.eq("user_state", "ACTIVE"),
										StructuredQuery.PropertyFilter.eq("user_role", "USER"),
										StructuredQuery.PropertyFilter.eq("user_profile", "public")))
												.build();

				users = datastore.run(query);

				List<UserInfo> listUSER = new ArrayList<>();

				users.forEachRemaining(u -> {
					listUSER.add(new UserInfo(u.getKey().getName(), u.getString("user_email"), u.getString("user_name")));
				});
				return Response.ok(g.toJson(listUSER)).build();
			case 2:
				query = Query.newEntityQueryBuilder()
						.setKind("User")
						.setFilter(StructuredQuery.PropertyFilter.eq("user_role", "USER"))
						.build();

				users = datastore.run(query);
				List<UserInfoFull> listGBO = new ArrayList<>();

				users.forEachRemaining(u -> {
					listGBO.add(new UserInfoFull(u.getKey().getName(), u.getString("user_email"), u.getString("user_name"), u.getString("user_profile"), u.getString("user_phone"), u.getString("user_cellPhone"), u.getString("user_address"), u.getString("user_addressC"), u.getString("user_cp"), u.getString("user_nif")));
				});
				return Response.ok(g.toJson(listGBO)).build();
			case 3:

				query = Query.newEntityQueryBuilder()
						.setKind("User")
						.setFilter(StructuredQuery.PropertyFilter.eq("user_role", "USER"))
						.build();

				Query<Entity> query2 = Query.newEntityQueryBuilder()
						.setKind("User")
						.setFilter(StructuredQuery.PropertyFilter.eq("user_role", "GBO"))
						.build();;

				users = datastore.run(query);
				List<UserInfoFull> listGS = new ArrayList<>();

				users.forEachRemaining(u -> {
					listGS.add(new UserInfoFull(u.getKey().getName(), u.getString("user_email"), u.getString("user_name"), u.getString("user_profile"), u.getString("user_phone"), u.getString("user_cellPhone"), u.getString("user_address"), u.getString("user_addressC"), u.getString("user_cp"), u.getString("user_nif")));
				});

				users = datastore.run(query2);

				users.forEachRemaining(u -> {
					listGS.add(new UserInfoFull(u.getKey().getName(), u.getString("user_email"), u.getString("user_name"), u.getString("user_profile"), u.getString("user_phone"), u.getString("user_cellPhone"), u.getString("user_address"), u.getString("user_addressC"), u.getString("user_cp"), u.getString("user_nif")));
				});
				return Response.ok(g.toJson(listGS)).build();
			case 4:
				query = Query.newEntityQueryBuilder()
						.setKind("User")
						.build();

				users = datastore.run(query);
				List<UserInfoFull> listSU = new ArrayList<>();

				users.forEachRemaining(u -> {
					listSU.add(new UserInfoFull(u.getKey().getName(), u.getString("user_email"), u.getString("user_name"), u.getString("user_profile"), u.getString("user_phone"), u.getString("user_cellPhone"), u.getString("user_address"), u.getString("user_addressC"), u.getString("user_cp"), u.getString("user_nif")));
				});
				return Response.ok(g.toJson(listSU)).build();
		}


		return Response.status(Status.BAD_REQUEST).entity("Wrong roles.").build();
	}

	@POST
	@Path("/logout")
	public Response logout(LogoutData data){

		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.tokenId);
		Entity token = datastore.get(tokenKey);

		Query<Entity> query = Query.newEntityQueryBuilder()
				.setKind("Token")
				.setFilter(StructuredQuery.PropertyFilter.eq("token_username", token.getString("token_username")))
				.build();

		QueryResults<Entity> tokens = datastore.run(query);

		Transaction txn = datastore.newTransaction();

		try{
			tokens.forEachRemaining(auxToken -> {
				txn.delete(auxToken.getKey());
			});
			txn.commit();
		}
		finally {
			if(txn.isActive()){
				txn.rollback();
			}
		}
		return Response.ok().build();
	}



	@POST
	@Path("/login")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response login(LoginData data){
		LOG.fine("Login attempt by user: " + data.username);

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Entity user = datastore.get(userKey);

		AuthToken authToken = new AuthToken(data.username, user.getString("user_role"));

		Key tokenId = datastore.newKeyFactory().setKind("Token").newKey(authToken.tokenID);
		Entity token = Entity.newBuilder(tokenId)
						.set("token_username", data.username)
						.set("token_role", authToken.role)
						.set("token_creation", authToken.creationDate)
						.set("token_expiration", authToken.expirationDate).build();

		Transaction txn = datastore.newTransaction();

		try{
			if(user != null){
				String password = user.getString("user_pwd");
				if(password.equals(DigestUtils.sha512Hex(data.password))){
					txn.add(token);
					txn.commit();
					LOG.info("User " + data.username + " logged in sucessfully.");
					return Response.ok(g.toJson(authToken.tokenID)).build();

				}
				else{
					txn.rollback();
					LOG.warning("Wrong password for username: " + data.username);
					return Response.status(Status.FORBIDDEN).build();
				}
			}
			else{
				txn.rollback();
				LOG.warning("Failed login attempt for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		finally {
			if(txn.isActive()){
				txn.rollback();
			}
		}
	}



}
