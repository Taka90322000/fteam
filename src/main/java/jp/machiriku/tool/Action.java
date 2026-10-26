package jp.machiriku.tool;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class Action {
	public abstract void execute(HttpServletRequest req, HttpServletResponse res) throws Exception;
}
