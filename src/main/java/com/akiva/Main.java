package com.akiva;

import com.akiva.model.CourseIdea;
import com.akiva.model.CourseIdeaDAO;
import com.akiva.model.NotFoundException;
import com.akiva.model.SimpleCourseIdeaDAO;
import spark.ModelAndView;
import spark.Request;
import spark.template.handlebars.HandlebarsTemplateEngine;
import java.util.HashMap;
import java.util.Map;
import static spark.Spark.*;

public class Main {

    private static final String FLASH_MESSAGE_KEY = "flash_message";
    public static void main(String[] args) {
        staticFileLocation("/public");
        CourseIdeaDAO dao = new SimpleCourseIdeaDAO();
        before((request, response) -> {
            if(request.cookie("username") != null) {
                request.attribute("username", request.cookie("username"));
            }
        });
        before("/ideas", (request, response) -> {
            if(request.attribute("username") == null) {
                selFlashMessage(request, "Whoops, pleas sign in first!");
                response.redirect("/");
                halt();
            }
        });
        get("/", (req, res) -> {
                 Map<String, String> model = new HashMap<>();
                 model.put("username", req.attribute("username"));
                 model.put("flashMessage", captureFlashMessage(req));
                 return new ModelAndView(model, "index.hbs");
            }, new HandlebarsTemplateEngine()
        );
        post("/sign-in", (req, res) -> {
                Map<String, String> model = new HashMap<>();
                String username = req.queryParams("username");
                res.cookie("username", username);
                model.put("username", username);
                res.redirect("/");
                return null;
            });
        get("/ideas", (request, response) ->  {
                Map<String, Object> model = new HashMap<>();
                model.put("ideas", dao.findAll());
                model.put("flashMessage", captureFlashMessage(request));
                return new ModelAndView(model, "ideas.hbs");
        }, new HandlebarsTemplateEngine()
        );
        post("/ideas", (request, response) -> {
            String title = request.queryParams("title");
            CourseIdea courseIdea = new CourseIdea(title,
                    request.attribute("username"));
            dao.add(courseIdea);
            response.redirect("/ideas");
            return null;
        });
        get("ideas/:slug", (request, response) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("idea", dao.findBySlug(request.params("slug")));
            return new ModelAndView(model, "idea.hbs");
        }, new HandlebarsTemplateEngine());
        post("ideas/:slug/vote", (request, response) -> {
            CourseIdea idea = dao.findBySlug(request.params("slug"));
            boolean added = idea.addVoter(request.attribute("username"));
            if(added) {
                selFlashMessage(request, "Thanks for your vote!");
            } else {
                selFlashMessage(request, "You already voted!");
            }
            response.redirect("/ideas");
            return null;
        });
        exception(NotFoundException.class, (exc, req, res) -> {
            res.status(404);
            HandlebarsTemplateEngine engine = new HandlebarsTemplateEngine();
            String html = engine.render(new ModelAndView(null, "not-found.hbs"));
            res.body(html);
        });
    }

    private static void selFlashMessage(Request request, String message) {
        request.session().attribute(FLASH_MESSAGE_KEY, message);
    }

    private static String getFlashMessage(Request request) {
        if(request.session(false) == null) {
            return null;
        }
        if(!request.session().attributes().contains(FLASH_MESSAGE_KEY)) {
            return null;
        }
        return request.session().attribute(FLASH_MESSAGE_KEY);
    }

    private static String captureFlashMessage(Request request) {
        String message = getFlashMessage(request);
        if(message != null) {
            request.session().removeAttribute(FLASH_MESSAGE_KEY);
        }
        return message;
    }


}
