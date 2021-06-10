<%--

    Copyright (C) 2016-2019 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<jsp:include page="/jsp/header.jsp"/>

<div class="container">
    <% request.setAttribute("adminActivePage", "adminAnalytics"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <h3>User Analytics</h3>

    <table id="tableUsers" class="table table-striped">
        <thead>
            <tr>
                <th class="toggle-all-details"><i class="toggle-details-icon fa fa-chevron-right"></i></th>
                <th>ID</th>
                <th>Username</th>
                <th>Games Played</th>
                <th>Attacker Score</th>
                <th>Defender Score</th>
                <th>Total Score</th>
            </tr>
        </thead>
    </table>

    <div class="row mt-4">
        <div class="col-auto">
            <div class="btn-group">
                <a download="user-analytics.csv" href="<%=request.getContextPath()+Paths.API_ANALYTICS_USERS%>?fileType=csv"
                   type="button" class="btn btn-sm btn-outline-secondary" id="download">
                    <i class="fa fa-download me-1"></i>
                    Download table
                </a>
                <a download="user-analytics.csv" href="<%=request.getContextPath()+Paths.API_ANALYTICS_USERS%>?fileType=csv"
                   type="button" class="btn btn-sm btn-outline-secondary" id="download-csv">
                    as CSV
                </a>
                <a download="user-analytics.json" href="<%=request.getContextPath()+Paths.API_ANALYTICS_USERS%>?fileType=json"
                   type="button" class="btn btn-sm btn-outline-secondary" id="download-json">
                    as JSON
                </a>
            </div>
        </div>
    </div>
</div>

<script src="js/datatables-utils.js" type="text/javascript" ></script>

<script>
(function () {

    function format(data) {
        return '' +
            `<div class="child-row-wrapper">
                <table class="child-row-details">
                    <thead>
                        <tr>
                            <th>Games Played</td>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>Games as Attacker:</td>
                            <td>\${dtValAndPercent(data.attackerGamesPlayed, data.gamesPlayed)}</td>
                            <td>Games as Defender:</td>
                            <td>\${dtValAndPercent(data.defenderGamesPlayed, data.gamesPlayed)}</td>
                        </tr>
                    </tbody>
                    <thead>
                        <tr>
                            <th>Mutants</td>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>Mutants Submitted:</td>
                            <td>\${data.mutantsSubmitted}</td>
                            <td>Per Game (as Attacker):</td>
                            <td>\${dtDiv(data.mutantsSubmitted, data.attackerGamesPlayed)}</td>
                        </tr>
                        <tr>
                            <td>Alive Mutants:</td>
                            <td>\${dtValAndPercent(data.mutantsAlive, data.mutantsSubmitted)}</td>
                            <td>Per Game (as Attacker):</td>
                            <td>\${dtDiv(data.mutantsAlive, data.attackerGamesPlayed)}</td>
                        </tr>
                        <tr>
                            <td>Equivalent Mutants:</td>
                            <td>\${dtValAndPercent(data.mutantsEquivalent, data.mutantsSubmitted)}</td>
                            <td>Per Game (as Attacker):</td>
                            <td>\${dtDiv(data.mutantsEquivalent, data.attackerGamesPlayed)}</td>
                        </tr>
                    </tbody>
                    <thead>
                        <tr>
                            <th>Tests</td>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>Tests Submitted:</td>
                            <td>\${data.testsSubmitted}</td>
                            <td>Per Game (as Defender):</td>
                            <td>\${dtDiv(data.testsSubmitted, data.defenderGamesPlayed)}</td>
                        </tr>
                        <tr>
                            <td>Mutants Killed:</td>
                            <td>\${data.mutantsKilled}</td>
                            <td>Per Game (as Defender):</td>
                            <td>\${dtDiv(data.mutantsKilled, data.defenderGamesPlayed)}</td>
                            <td>Per Test:</td>
                            <td>\${dtDiv(data.mutantsKilled, data.testsSubmitted)}</td>
                        </tr>
                    </tbody>
                </table>
            </div>`;
    }

    $(document).ready(function() {
        const table = $('#tableUsers').DataTable({
            "ajax": {
                "url": "<%=request.getContextPath() + Paths.API_ANALYTICS_USERS + "?fileType=json"%>",
                "dataSrc": "data"
            },
            "columns": [
                {
                    "className":      'toggle-details',
                    "orderable":      false,
                    "data":           null,
                    "defaultContent": '<i class="toggle-details-icon fa fa-chevron-right"></i>'
                },
                { "data": "id" },
                { "data": "username" },
                { "data": "gamesPlayed" },
                { "data": "attackerScore" },
                { "data": "defenderScore" },
                { "data":
                    function (row, type, val, meta) {
                        return row.attackerScore + row.defenderScore;
                    }
                }
            ],
            "pageLength": 50,
            "order": [[ 1, "asc" ]],
            "scrollY": '600px',
            "scrollCollapse": true,
            "paging": false,
            "language": {"info": "Showing _TOTAL_ entries"}
        });

        setupChildRows(table, format);
    });

})();
</script>

<%@ include file="/jsp/footer.jsp" %>
