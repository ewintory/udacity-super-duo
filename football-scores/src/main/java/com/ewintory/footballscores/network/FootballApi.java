/*
 * Copyright 2015.  Emin Yahyayev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ewintory.footballscores.network;

import com.ewintory.footballscores.network.model.FixturesResponse;
import com.ewintory.footballscores.network.model.Season;
import com.ewintory.footballscores.network.model.TeamResponse;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

public interface FootballApi {

    @GET("/soccerseasons") List<Season> seasons();

    @GET("/soccerseasons/{id}/teams") TeamResponse teams(
            @Path("id") String seasonId);

    @GET("/fixtures") FixturesResponse fixtures(
            @Query("timeFrame") String timeFrame);

}
