package pt.ulisboa.tecnico.transparency.verifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pt.ulisboa.tecnico.transparency.verifier.contract.UserOuterClass.Authorization;
import pt.ulisboa.tecnico.transparency.verifier.contract.UserOuterClass.Credentials;

@RestController
@RequestMapping(path = "v2")
public class UserController {
  private final UserService userService;

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }

  @RequestMapping(value = "/user/login", method = RequestMethod.POST)
  public Authorization login(@RequestBody Credentials credentials) {
    return userService.login(credentials);
  }

  @RequestMapping(value = "/user/register", method = RequestMethod.POST)
  public Authorization register(@RequestBody Credentials credentials) {
    return userService.register(credentials);
  }

  @RequestMapping(value = "/token/refresh", method = RequestMethod.GET)
  public Authorization refresh() {
    return userService.refresh();
  }

}
