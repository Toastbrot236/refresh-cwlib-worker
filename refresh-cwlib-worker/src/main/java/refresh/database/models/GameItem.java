package refresh.database.models;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GameItem {
    public String PlanHash;
    public String IconHash;
    public String Title;
    public String Description;
    public String CreatorName;
    public String[] ContributorNames;
    public boolean IsGamePhoto;
    public boolean IsCameraPhoto;
    public boolean IsUserCreation;

    public GameItem(ResultSet rs) throws SQLException {
        this.PlanHash = rs.getString(1);
        this.IconHash = rs.getString(2);
        this.Title = rs.getString(3);
        this.Description = rs.getString(4);
        this.CreatorName = rs.getString(5);
        this.ContributorNames = (String[])rs.getArray(6).getArray();
        this.IsGamePhoto = rs.getBoolean(7);
        this.IsCameraPhoto = rs.getBoolean(8);
        this.IsUserCreation = rs.getBoolean(9);
    }

    public GameItem(String pPlanHash, String pIconHash, String pTitle, String pDescription, 
        String pCreatorName, String[] pContributorNames, boolean pIsGamePhoto, boolean pIsCameraPhoto, 
        boolean pIsUserCreation) {

        this.PlanHash = pPlanHash;
        this.IconHash = pIconHash;
        this.Title = pTitle;
        this.Description = pDescription;
        this.CreatorName = pCreatorName;
        this.ContributorNames = pContributorNames;
        this.IsGamePhoto = pIsGamePhoto;
        this.IsCameraPhoto = pIsCameraPhoto;
        this.IsUserCreation = pIsUserCreation;
    }
}
