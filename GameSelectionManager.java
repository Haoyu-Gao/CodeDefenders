package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.sql.*;

public class GameSelectionManager extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        RequestDispatcher dispatcher = request.getRequestDispatcher("html/user_games_view.jsp");
        dispatcher.forward(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        switch (request.getParameter("formType")) {
            
            case "createGame" :

                String classId = (String)request.getParameter("class");
                String role = (String)request.getParameter("role");
                String rounds = (String)request.getParameter("rounds");

                int uid = (Integer)request.getSession().getAttribute("uid");

                System.out.println(classId);

                Connection conn = null;
                Statement stmt = null;
                String sql = null;

                try {
                    Class.forName("com.mysql.jdbc.Driver");
                    conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

                    stmt = conn.createStatement();
                    sql = String.format("INSERT INTO games (%s, FinalRound, Class_ID) VALUES ('%d', '%s', '%s');", role, uid, rounds, classId);
                    stmt.execute(sql);

                    stmt.close();
                    conn.close();

                } catch(SQLException se) {
                    System.out.println(se);
                    //Handle errors for JDBC
                    se.printStackTrace();
                } catch(Exception e) {
                    System.out.println(e);
                    //Handle errors for Class.forName
                    e.printStackTrace();
                } finally{
                    //finally block used to close resources
                    try {
                        if(stmt!=null)
                           stmt.close();
                    } catch(SQLException se2) {}// nothing we can do

                    try {
                        if(conn!=null)
                        conn.close();
                    } catch(SQLException se) {
                        se.printStackTrace();
                    }//end finally try
                } //end try
                break;
        }

        doGet(request, response);
    }

    public static ArrayList<Game> getGamesForUser(int userId) {
        Connection conn = null;
        Statement stmt = null;
        String sql = null;
        ArrayList<Game> gameList = new ArrayList<Game>();

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("SELECT * FROM games WHERE Attacker_ID=%d OR Defender_ID=%d;", userId, userId);
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                gameList.add(new Game(rs.getInt("Game_ID"), rs.getInt("Attacker_ID"), rs.getInt("Defender_ID"), rs.getInt("Class_ID"),
                    rs.getInt("CurrentRound"), rs.getInt("FinalRound"), rs.getString("ActivePlayer"), rs.getString("State")));
            }

            stmt.close();
            conn.close();
            

        } catch(SQLException se) {
            System.out.println(se);
            //Handle errors for JDBC
            se.printStackTrace();
        } catch(Exception e) {
            System.out.println(e);
            //Handle errors for Class.forName
            e.printStackTrace();
        } finally{
            //finally block used to close resources
            try {
                if(stmt!=null)
                   stmt.close();
            } catch(SQLException se2) {}// nothing we can do

            try {
                if(conn!=null)
                conn.close();
            } catch(SQLException se) {
                se.printStackTrace();
            }//end finally try
        } //end try

        return gameList;
    }
}