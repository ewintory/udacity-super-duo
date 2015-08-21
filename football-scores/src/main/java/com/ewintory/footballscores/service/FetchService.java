package com.ewintory.footballscores.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.util.Log;

import com.ewintory.footballscores.BuildConfig;
import com.ewintory.footballscores.network.FootballApi;
import com.ewintory.footballscores.network.model.Fixture;
import com.ewintory.footballscores.network.model.FixturesResponse;
import com.ewintory.footballscores.network.model.HrefWrapper;
import com.ewintory.footballscores.network.model.Season;
import com.ewintory.footballscores.network.model.Team;
import com.ewintory.footballscores.network.model.TeamResponse;
import com.ewintory.footballscores.provider.ScoresContract;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import retrofit.Endpoints;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;


public final class FetchService extends IntentService {
    private static final String TAG = FetchService.class.getSimpleName();

    private static final String API_URL = "http://api.football-data.org/alpha";
    private static final String SEASON_LINK = API_URL + "/soccerseasons/";
    private static final String MATCH_LINK = API_URL + "/fixtures/";
    private static final String TEAMS_LINK = API_URL + "/teams/";

    private final FootballApi mFootballApi;

    private Map<String, Season> mSeasonsMap;
    private Map<String, Team> mTeamsMap;

    public FetchService() {
        super(TAG);

        final RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(Endpoints.newFixedEndpoint(API_URL))
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override public void intercept(RequestFacade request) {
                        request.addHeader("X-Auth-Token", BuildConfig.FOOTBALL_DATA_API_KEY);
                    }
                })
                .build();

        mFootballApi = restAdapter.create(FootballApi.class);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        fetchFixtures("n3");
        fetchFixtures("p3");
    }

    private void fetchFixtures(String timeFrame) {
        FixturesResponse response = null;

        try {
            response = mFootballApi.fixtures(timeFrame);
        } catch (Exception e) {
            Log.e(TAG, "Exception here" + e.getMessage());
        }

        if (response != null)
            processFixturesResponse(response);
    }

    private void processFixturesResponse(final FixturesResponse fixturesResponse) {
        final List<Fixture> fixtures = fixturesResponse.getFixtures();
        Log.v(TAG, String.format("Processing fixtures, %d total items.", fixtures.size()));

        final Map<String, Team> teamMap = getTeamsMap();
        final Map<String, Season> seasonMap = getSeasonsMap();
        final Map<String, Integer> matchDateMap = new HashMap<>(); // for testing

        //Match data
        String matchId = null;
        String league = null;
        String leagueCaption = null;
        String matchDate = null;
        String matchTime = null;
        String homeTeamCrest = null;
        String awayTeamCrest = null;
        String homeTeamName = null;
        String awayTeamName = null;
        Long goalsHomeTeam = null;
        Long goalsAwayTeam = null;
        Long matchDay = null;

        //ContentValues to be inserted
        Vector<ContentValues> values = new Vector<>(fixtures.size());
        for (Fixture fixture : fixtures) {
            matchId = extractId(fixture.getLinks().getSelf(), MATCH_LINK);
            league = extractId(fixture.getLinks().getSoccerSeason(), SEASON_LINK);

            if (seasonMap.containsKey(league)) {
                leagueCaption = seasonMap.get(league).getCaption();
                matchDate = fixture.getDate();
                matchTime = matchDate.substring(matchDate.indexOf("T") + 1, matchDate.indexOf("Z"));
                matchDate = matchDate.substring(0, matchDate.indexOf("T"));
                SimpleDateFormat matchDateFormat = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss", Locale.US);
                matchDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                try {
                    Date parseddate = matchDateFormat.parse(matchDate + matchTime);
                    SimpleDateFormat new_date = new SimpleDateFormat("yyyy-MM-dd:HH:mm", Locale.US);
                    new_date.setTimeZone(TimeZone.getDefault());
                    matchDate = new_date.format(parseddate);
                    matchTime = matchDate.substring(matchDate.indexOf(":") + 1);
                    matchDate = matchDate.substring(0, matchDate.indexOf(":"));
                } catch (Exception e) {
                    Log.d(TAG, "error here!");
                    Log.e(TAG, e.getMessage());
                }

                homeTeamName = fixture.getHomeTeamName();
                awayTeamName = fixture.getAwayTeamName();

                homeTeamCrest = teamMap.get(extractId(fixture.getLinks().getHomeTeam(), TEAMS_LINK)).getCrestUrl();
                awayTeamCrest = teamMap.get(extractId(fixture.getLinks().getAwayTeam(), TEAMS_LINK)).getCrestUrl();

                goalsHomeTeam = fixture.getResult().getGoalsHomeTeam();
                goalsAwayTeam = fixture.getResult().getGoalsAwayTeam();

                matchDay = fixture.getMatchDay();
                if (!matchDateMap.containsKey(matchDate)) {
                    matchDateMap.put(matchDate, 1);
                } else {
                    matchDateMap.put(matchDate, matchDateMap.get(matchDate) + 1);
                }

                ContentValues matchValues = new ContentValues();
                matchValues.put(ScoresContract.ScoreEntry.COLUMN_MATCH_ID, matchId);
                matchValues.put(ScoresContract.ScoreEntry.COLUMN_DATE, matchDate);
                matchValues.put(ScoresContract.ScoreEntry.COLUMN_TIME, matchTime);
                matchValues.put(ScoresContract.ScoreEntry.COLUMN_HOME, homeTeamName);
                matchValues.put(ScoresContract.ScoreEntry.COLUMN_AWAY, awayTeamName);
                matchValues.put(ScoresContract.ScoreEntry.COLUMN_HOME_CREST, homeTeamCrest);
                matchValues.put(ScoresContract.ScoreEntry.COLUMN_AWAY_CREST, awayTeamCrest);
                matchValues.put(ScoresContract.ScoreEntry.COLUMN_HOME_GOALS, goalsHomeTeam);
                matchValues.put(ScoresContract.ScoreEntry.COLUMN_AWAY_GOALS, goalsAwayTeam);
                matchValues.put(ScoresContract.ScoreEntry.COLUMN_LEAGUE, league);
                matchValues.put(ScoresContract.ScoreEntry.COLUMN_LEAGUE_CAPTION, leagueCaption);
                matchValues.put(ScoresContract.ScoreEntry.COLUMN_MATCH_DAY, matchDay);

                values.add(matchValues);
            } else {
                Log.w(TAG, "Invalid league id=" + league);
            }
        }

        for (String date : matchDateMap.keySet()) {
            Log.v(TAG, String.format("Fetched %d matches for date %s", matchDateMap.get(date), date));
        }

        ContentValues[] contentValues = new ContentValues[values.size()];
        values.toArray(contentValues);
        int insertedData = getContentResolver().bulkInsert(ScoresContract.BASE_CONTENT_URI, contentValues);

        Log.v(TAG, "Successfully Inserted : " + String.valueOf(insertedData));
    }

    private Map<String, Season> getSeasonsMap() {
        if (mSeasonsMap == null) {
            final List<Season> seasons = mFootballApi.seasons();
            Log.v(TAG, String.format("Seasons loaded, %d items", seasons.size()));

            String seasonId = null;

            mSeasonsMap = new HashMap<>(seasons.size());
            for (Season season : seasons) {
                seasonId = season.getLinks().getSelf().getHref();
                seasonId = seasonId.replace(SEASON_LINK, "");
                mSeasonsMap.put(seasonId, season);
            }
        }
        return mSeasonsMap;
    }

    private Map<String, Team> getTeamsMap() {
        if (mTeamsMap == null) {
            final Map<String, Season> seasonMap = getSeasonsMap();

            Map<String, Team> teamMap = new HashMap<>(100);

            for (String seasonId : seasonMap.keySet()) {
                TeamResponse teamResponse = mFootballApi.teams(seasonId);
                for (Team team : teamResponse.getTeams()) {
                    teamMap.put(extractId(team.getLinks().getSelf(), TEAMS_LINK), team);
                }
            }

            mTeamsMap = teamMap;
            Log.v(TAG, String.format("Teams fetched, %d total items.", mTeamsMap.size()));
        }
        return mTeamsMap;
    }

    private static String extractId(HrefWrapper hrefWrapper, String link) {
        String id = hrefWrapper.getHref();
        return id.replace(link, "");
    }
}

