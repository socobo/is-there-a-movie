package com.botomo.data;

import static com.botomo.routes.EventBusAddresses.GET_ALL;
import static com.botomo.routes.EventBusAddresses.SEARCH;

import java.util.List;
import java.util.stream.Collectors;

import com.botomo.models.Book;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * This verticle realizes CRUD functionality for the book entity. To access
 * these functionalities use the message defined in
 * {@link com.botomo.data.CrudAddresses}
 *
 */
public class BookCrudVerticle extends AbstractVerticle {

	private static final String COLLECTION = "books";
	private MongoClient mongo = null;

	@Override
	public void start(Future<Void> fut) throws Exception {

		// Initialize mongo client
		mongo = MongoClient.createShared(vertx, config());

		/**
		 * Consumer to get all books from the db.
		 */
		vertx.eventBus().consumer(GET_ALL, message -> {
			mongo.find(COLLECTION, new JsonObject(), result -> {
				// Fetch all books from db
				List<JsonObject> foundJsonObjects = result.result();
				// Map JsonObjects to Book Objects
				List<Book> books = foundJsonObjects.stream().map(Book::new).collect(Collectors.toList());
				System.out.println("books: " + books);
				// Return the fetched books as json
				message.reply(Json.encodePrettily(books));
			});
		});

		/**
		 * Consumer to get only searched books from the db. The following fields
		 * of the book entity are considered for the search: title, author,
		 * subtitle, year. This method doesn't provide a full text search.
		 * Instead it realizes a like-wise search.
		 * 
		 */
		vertx.eventBus().consumer(SEARCH, message -> {
			final String searchTerm = (String) message.body();

			mongo.find(COLLECTION, buildSearchQuery(searchTerm), result -> {
				// Fetch all books from db
				List<JsonObject> foundJsonObjects = result.result();
				// Map JsonObjects to Book Objects
				List<Book> books = foundJsonObjects.stream().map(Book::new).collect(Collectors.toList());
				System.out.println("books: " + books);
				// Return the fetched books as json
				message.reply(Json.encodePrettily(books));
			});
		});
		
		fut.complete();
		
	}

	private JsonObject buildSearchQuery(final String searchTerm) {

		// Build the json query
		final String pattern = ".*" + searchTerm + ".*";

		JsonObject q = new JsonObject();

		JsonObject patternWithOption = new JsonObject();
		patternWithOption.put("$regex", pattern);
		patternWithOption.put("$options", "i");

		JsonArray searchFields = new JsonArray();
		searchFields.add(new JsonObject().put("title", patternWithOption));
		searchFields.add(new JsonObject().put("author", patternWithOption));
		searchFields.add(new JsonObject().put("subtitle", patternWithOption));
		searchFields.add(new JsonObject().put("year", patternWithOption));

		q.put("$or", searchFields);

		return q;
	}

}
