package com.akiva;

import com.akiva.model.CourseIdea;
import com.akiva.model.CourseIdeaDAO;
import com.akiva.model.SimpleCourseIdeaDAO;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;
import java.util.HashMap;
import java.util.Map;
import static spark.Spark.*;

public class Main {

    public static void main(String[] args) {
        staticFileLocation("/public");
        CourseIdeaDAO dao = new SimpleCourseIdeaDAO();
        before((request, response) -> {
            if(request.cookie("username") != null) {
                request.attribute("username", request.cookie("username"));
            }
        });
        before("/ideas", (request, response) -> {
            //todo add message when redirect
            if(request.attribute("username") == null) {
                response.redirect("/");
                halt();
            }
        });
        get("/", (req, res) -> {
                 Map<String, String> model = new HashMap<>();
                 model.put("username", req.attribute("username"));
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
            idea.addVoter(request.attribute("username"));
            response.redirect("/ideas");
            return null;
        });

    }

}
