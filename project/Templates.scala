
import au.com.langdale.soapbox._
import sbt._
import Publisher._


object Templates extends Plugin {
  override def projectSettings = Seq(
    siteTemplates += Template("*.md",
      (title, content) => {
        <html>
            <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>{title}</title>

                <!-- Google Fonts -->
                <link href="http://fonts.googleapis.com/css?family=Poiret+One|PT+Serif|Open+Sans:400,300" rel="stylesheet" type="text/css" />

                <!-- Stylesheets -->
                <link href="bootstrap.min.css" rel="stylesheet" />
                <link href="bootstrap-responsive.min.css" rel="stylesheet" />
                <link href="socialicons.css" rel="stylesheet" />
                <link href="glyphicons.css" rel="stylesheet" />
                <link href="halflings.css" rel="stylesheet" />
                <link href="template.css" rel="stylesheet" />
                <link href="colors/color-classic.css" rel="stylesheet" id="colorcss" />

                <!-- Modernizr for Glyphicons (SVG) -->
                <script src="modernizr.js"></script>

                <!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
                <!--[if lt IE 9]>
                <script src="js/html5shiv.js"></script>
                <![endif]-->
            </head>
            <body>
                <div class="container">
                    <div class="masthead clearfix">
                        <a href="index.html">
                            <h1>Thomas Toye</h1>
                        </a>
                        <ul id="nav" class="nav ww-nav pull-right hidden-phone">
                        {
                            for((text, target) <- siteMenu.value.entries) yield {
                                <li>
                                    <a href={target} class="menu">{text}</a>
                                </li>
                            }
                        }
                        </ul>
                    </div>
                    <hr/>
                    <div class="main-content">
                        <div class="row">
                            {content}
                        </div>
                    </div>
                    { Disqus.disqusFragment.value }
                    <div id="footer">{ siteFooter.value }</div>
                </div>

                { Twitter.tweetScript.value }
                { GoogleAnalytics.analyticsFragment.value }

                <!-- Javascript -->
                <script src="jquery-1.9.1.js"></script>
                <script src="bootstrap.js"></script>
                <script src="tinynav.js"></script>
                <script src="template.js"></script>
                <script src="highlightjs/highlight.pack.js"></script>
                <script>hljs.initHighlightingOnLoad();</script>
                <!--<script src="js/bootstrap.min.js"></script>-->
            </body>
        </html>
      }
    )
  )
}
