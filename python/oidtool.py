	# -*- coding: utf-8 -*-
2	
3	__version__ = "0.2"
4	__authors__ = ["Sylvain Hellegouarch (sh@defuze.org)"]
5	__date__ = "2008/05/15"
6	__copyright__ = """
7	Copyright (c) 2006, 2007, 2008 Sylvain Hellegouarch
8	All rights reserved.
9	"""
10	__license__ = """
11	Redistribution and use in source and binary forms, with or without modification,
12	are permitted provided that the following conditions are met:
13	 
14	     * Redistributions of source code must retain the above copyright notice,
15	       this list of conditions and the following disclaimer.
16	     * Redistributions in binary form must reproduce the above copyright notice,
17	       this list of conditions and the following disclaimer in the documentation
18	       and/or other materials provided with the distribution.
19	     * Neither the name of Sylvain Hellegouarch nor the names of his contributors
20	       may be used to endorse or promote products derived from this software
21	       without specific prior written permission.
22	 
23	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
24	ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
25	WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
26	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
27	FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
28	DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
29	SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
30	CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
31	OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
32	OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
33	"""
34	
35	__doc__ = """OpenID tool for CherryPy 3"""
36	
37	from cgi import escape
38	
39	# Requires at least Python OpenID 1.1.2RC1
40	# http://www.openidenabled.com/resources/downloads/python-openid/
41	import openid
42	from openid.consumer import consumer
43	from openid.oidutil import appendArgs
44	from openid.cryptutil import randomString
45	
46	# Requires at least Python Yadis 1.1.0RC1
47	# http://www.openidenabled.com/resources/downloads/python-openid/
48	from yadis.discover import DiscoveryFailure
49	
50	# Requires at least Python URLJR 1.0.1
51	# http://www.openidenabled.com/resources/downloads/python-openid/
52	from urljr.fetchers import HTTPFetchingError
53	
54	# Requires CP >= 3.0.0
55	import cherrypy
56	
57	UNKNOWN = 0
58	PROCESSING = 1
59	AUTHENTICATED = 2
60	
61	DEFAULT_SESSION_NAME = 'OpenIDTool'
62	
63	__all__ = ['OpenIDTool']
64	
65	class OpenIDTool:
66	    def __init__(self, store, base_auth_path, session_name=DEFAULT_SESSION_NAME):
67	        """
68	        This tool provides a fairly easy way to add a OpenID consumer to your CherryPy
69	        application.
70	
71	        When receiving a request the following steps happened:
72	
73	        1.a. If the request path starts with 'base_auth_path', the tool returns immediatly
74	        1.b. The tool looks for the openid_url parameters (either passed within the querystring
75	             or through the request body).
76	             a. If that parameter is not present the tool immediatly redirects to the login page
77	             b. If it is found then it redirects to the OpenID provider service
78	        2. When the provider service returns the tool checks if the authorization was given
79	           a. If not then it calls the appropriate error handler (failure, cancelled, error)
80	           b. If the authorization was successful then it processes as normal the requested
81	              page handler.
82	
83	        At each step the current status of the processing is kept within the session so that
84	        we know where we stand whenever we received a request.
85	
86	        Keyword arguments:
87	        store -- a Python OpenID store instance (check the OpenID documentation)
88	        
89	        base_auth_path -- base path of the objects containing page handlers for each step
90	        in the process (login, logout, failure, cancelled, error). Note that setting this
91	        value might be done differently in the future as the current practice is not
92	        really flexible.
93	        
94	        session_name -- name to give to the session node within the session object (when using
95	        a cookie this is the cookie name for instance)
96	        """
97	        self.store = store
98	        self.session_name = session_name
99	        self.base_auth_path = base_auth_path + '/'
100	        self.login_path = '%s/login' % base_auth_path
101	        self.failed_authentication_path = '%s/failure' % base_auth_path
102	        self.cancel_authentication_path = '%s/cancelled' % base_auth_path
103	        self.error_authentication_path = '%s/error' % base_auth_path
104	       
105	    def get_session(self):
106	        oidsession = cherrypy.session.get(self.session_name, None)
107	       
108	        if not oidsession or not isinstance(oidsession, dict):
109	            oidsession = {}
110	           
111	        if 'sid' not in oidsession:
112	            sid = randomString(16, '0123456789abcdef')
113	            oidsession['sid'] = sid
114	            cherrypy.session[self.session_name] = oidsession
115	            cherrypy.session[self.session_name]['status'] = UNKNOWN
116	       
117	        return cherrypy.session[self.session_name]
118	
119	    def is_processing(self):
120	        if cherrypy.session.has_key(self.session_name):
121	            if 'status' in cherrypy.session[self.session_name]:
122	                if cherrypy.session[self.session_name]['status'] in [PROCESSING, AUTHENTICATED]:
123	                    return True
124	        return False
125	
126	    def is_authenticated(self):
127	        if cherrypy.session.has_key(self.session_name):
128	            if 'status' in cherrypy.session[self.session_name]:
129	                if cherrypy.session[self.session_name]['status'] == AUTHENTICATED:
130	                    return True
131	        return False
132	
133	    def verify(self):
134	        """
135	        First part of the OpenID processing. We get the OpenID url and
136	        we redirect the user-agent to that url for authentication.
137	        """
138	        # If the requested path belongs to the one defined for the
139	        # connection handlers then we do not performany verification
140	        if cherrypy.request.path_info.startswith(self.base_auth_path):
141	            return
142	       
143	        # this method is always called so we check if we haven't already
144	        # been authenticated or if we are not in the middle of
145	        # the processing and leave silentely in such case
146	        if self.is_processing():
147	            return
148	       
149	        openid_url = cherrypy.request.params.get('openid_url', None)
150	        if not openid_url:
151	            raise cherrypy.HTTPRedirect(self.login_path)
152	
153	        del cherrypy.request.params['openid_url']
154	
155	        oidconsumer = consumer.Consumer(self.get_session(), self.store)
156	        try:
157	            request = oidconsumer.begin(openid_url)
158	        except HTTPFetchingError, exc:
159	            # these could be more explicit maybe
160	            raise HTTPError(500, 'Error in discovery')
161	        except DiscoveryFailure, exc:
162	            # these could be more explicit maybe
163	            raise HTTPError(500, 'Error in discovery')
164	        else:
165	            if request is None:
166	                # these could be more explicit maybe
167	                raise HTTPError(500, 'No OpenID service found')
168	            else:
169	                # Then, ask the library to begin the authorization.
170	                # Here we find out the identity server that will verify the
171	                # user's identity, and get a token that allows us to
172	                # communicate securely with the identity server.
173	            
174	                return_to = cherrypy.url(cherrypy.request.path_info)
175	                redirect_url = request.redirectURL(cherrypy.request.base, return_to)
176	
177	                cherrypy.session[self.session_name]['return_to'] = return_to
178	                cherrypy.session[self.session_name]['status'] = PROCESSING
179	                raise cherrypy.HTTPRedirect(redirect_url)
180	
181	    def process(self):
182	        """
183	        Second part of the authentication. The OpenID provider service
184	        redirects to us with information in the URL regarding the status
185	        of the authentication on its side.
186	        """
187	        # If the requested path belongs to the one defined for the
188	        # connection handlers then we do not performany verification
189	        if cherrypy.request.path_info.startswith(self.base_auth_path):
190	            return
191	       
192	        # If we are already authenticated then we don't apply this step
193	        # any further
194	        if self.is_authenticated():
195	            return
196	       
197	        oidconsumer = consumer.Consumer(self.get_session(), self.store)
198	
199	        cherrypy.session[self.session_name]['status'] = UNKNOWN
200	       
201	        # Ask the library to check the response that the server sent
202	        # us.  Status is a code indicating the response type. info is
203	        # either None or a string containing more information about
204	        # the return type.
205	        info = oidconsumer.complete(cherrypy.request.params,
206	                                    return_to=cherrypy.session[self.session_name]['return_to'])
207	        cherrypy.session[self.session_name]['info'] = info
208	        if info.status == consumer.FAILURE and info.identity_url:
209	            # In the case of failure, if info is non-None, it is the
210	            # URL that we were verifying. We include it in the error
211	            # message to help the user figure out what happened.
212	            raise cherrypy.HTTPRedirect(self.failed_authentication_path)
213	        elif info.status == consumer.SUCCESS:
214	            # Success means that the transaction completed without
215	            # error. If info is None, it means that the user cancelled
216	            # the verification.
217	            
218	            # This is a successful verification attempt. If this
219	            # was a real application, we would do our login,
220	            # comment posting, etc. here.
221	            if info.endpoint.canonicalID:
222	                # You should authorize i-name users by their canonicalID,
223	                # rather than their more human-friendly identifiers.  That
224	                # way their account with you is not compromised if their
225	                # i-name registration expires and is bought by someone else.
226	                pass
227	            cherrypy.session[self.session_name]['status'] = AUTHENTICATED
228	            cherrypy.request.params = {}
229	            raise cherrypy.HTTPRedirect(cherrypy.url(cherrypy.request.path_info))
230	        elif info.status == consumer.CANCEL:
231	            # cancelled
232	            raise cherrypy.HTTPRedirect(self.cancel_authentication_path)
233	       
234	        raise cherrypy.HTTPRedirect(self.error_authentication_path)
235	       
236	    def _setup(self):
237	        # By setting these priorities we ensure that 'verify' will be performed
238	        # before 'process'.
239	        cherrypy.request.hooks.attach('before_request_body', self.verify, priority=60)
240	        cherrypy.request.hooks.attach('before_request_body', self.process, priority=65)
