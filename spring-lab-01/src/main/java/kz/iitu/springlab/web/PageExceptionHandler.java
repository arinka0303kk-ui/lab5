package kz.iitu.springlab.web;

import kz.iitu.springlab.catalog.BookNotFoundException;
import kz.iitu.springlab.catalog.BookPageController;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(assignableTypes = BookPageController.class)
public class PageExceptionHandler {
    @ExceptionHandler(BookNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(BookNotFoundException ex, Model model) {
        model.addAttribute("bookId", ex.getId());
        return "error/not-found";
    }
}
