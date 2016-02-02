package playground.security;

import java.util.Base64;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

public class HttpBasicFilter implements WebFilter {

	HttpSessionSecurityContextRepository repository = new HttpSessionSecurityContextRepository();

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		String authorization = request.getHeaders().getFirst("Authorization");
		if(authorization != null) {
			String credentials = authorization.substring("Basic ".length(), authorization.length());
			byte[] decodedCredentials = Base64.getDecoder().decode(credentials);
			String decodedAuthz = new String(decodedCredentials);
			String[] userParts = decodedAuthz.split(":");
			String username = userParts[0];
			String password = userParts[1];

			if(userParts.length == 2 && username.equals(password)) {
				SecurityContext context = new SecurityContextImpl();
				context.setAuthentication(new UsernamePasswordAuthenticationToken(username, password, AuthorityUtils.createAuthorityList("ROLE_USER")));
				repository.save(exchange, context);
				return chain.filter(exchange);
			}
		}
		SecurityContext context = repository.load(exchange);
		if(context != null) {
			return chain.filter(exchange);
		}
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		response.getHeaders().set("WWW-Authenticate", "Basic realm=\"Reactive\"");
		return Mono.empty();
	}
}