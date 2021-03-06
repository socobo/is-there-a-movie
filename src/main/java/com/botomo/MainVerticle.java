package com.botomo;

import com.botomo.data.BookCrudVerticle;
import com.botomo.handlers.BookHandler;
import com.botomo.handlers.GoodreadsHandler;
import com.botomo.routes.Routing;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.FutureFactoryImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class MainVerticle extends AbstractVerticle {

    private enum Config { port, dbname, dburl }

    @Override
	public void start(Future<Void> startFuture) throws Exception {
        // create router
		Router router = Router.router(vertx);
		// create Book handler 
		BookHandler bookHandler = new BookHandler(vertx);

        GoodreadsHandler goodreadsHandler = new GoodreadsHandler(vertx);
		// create Routing
		Routing routing = new Routing(router, bookHandler, goodreadsHandler);
		// create server

		// Define Future for http sever
		Future<HttpServer> httpServerFut = Future.future();
		vertx
			.createHttpServer()
			.requestHandler(routing.register()::accept)
			.listen(Integer.getInteger(Config.port.name(), 8080), result -> {
				if (result.succeeded()) {
					System.out.println("HttpServer started");
					httpServerFut.completer();
				} else {
					System.out.println("Failed to start HttpServer");
					httpServerFut.fail(result.cause());
				}
			});

		// Define Future for BookCrudVerticle deployment

		Future<BookCrudVerticle> bookCrudVerticalFut = Future.future();
		this.deployBookCrudVerticle(bookCrudVerticalFut);

        Future<Void> goodreadsVerticleFuture = Future.future();
        this.deployGoodreadsVerticle(goodreadsVerticleFuture);

		CompositeFuture.all(httpServerFut, bookCrudVerticalFut, goodreadsVerticleFuture).setHandler(ar -> {
			if(ar.succeeded()){
				startFuture.complete();
			}else{
				startFuture.fail(ar.cause());
			}
		});
	}
    
    @Override
    public void stop() throws Exception {
    	System.out.println("MainVerticle stopped");
    }

    private void deployGoodreadsVerticle(Future<Void> fut) {
		DeploymentOptions options = new DeploymentOptions()
				.setInstances(2)
				.setWorker(true);
        vertx.deployVerticle("GoodReadsVerticle.rb", options, result -> {
            if (result.failed()) {
                result.cause().printStackTrace();
                fut.fail(result.cause());
            } else {
                fut.completer();
            }
        });
    }

    private void deployBookCrudVerticle(Future<BookCrudVerticle> fut){
    	JsonObject dbConf = new JsonObject()
    			.put("db_name", System.getProperty(Config.dbname.name()))
    			.put("connection_string", System.getProperty(Config.dburl.name()));
    	DeploymentOptions dbOpt = new DeploymentOptions()
				.setConfig(dbConf);


    	// deploy book crud verticle
    	vertx.deployVerticle(BookCrudVerticle.class.getName(), dbOpt, ar -> {
    		if (ar.failed()){
    			System.out.println("Failed to deploy BookCrudVerticle");
    			ar.cause().printStackTrace();
    			fut.fail(ar.cause());
    		} else {
    			// Replace this with some loggin framework
    			System.out.println("BookCrudVerticle successfully deployed");
    			fut.completer();
    		}
    	});
    }
}
