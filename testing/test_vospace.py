
"""
Test VOSpace API interface
"""
import os
import time
import unittest
import requests

SERVICE_PORT = os.environ.get("PUBLISH_PORT", "3002")
SERVICE_ORIGIN = f"http://127.0.0.1:{SERVICE_PORT}"

class TestUser:
    """
    Represents a file service user for testing purposes
    """
    def __init__(self, username: str):
        self.name = username
        self.token = f"${self.name}.123.123.faketoken"

class VosXML:
    """
    Provides various methods for generating various VO XML resources
    """
    @staticmethod
    def container_node(user: str, dir: str):
        """
        Return the XML to generate a new container node
        """
        std_namespace = "http://www.w3.org/2001/XMLSchema-instance"
        namespace = "http://www.ivoa.net/xml/VOSpace/v2.0"
        schema_type = "vos:ContainerNode"
        base_uri = "vos://datalab.noirlab!vospace"
        uri = f"{base_uri}/{user}/{dir}"

        return (
            f"<node \
                xmlns:xsi='{std_namespace}' \
                xmlns='{namespace}' \
                xsi:type='{schema_type}' \
                uri='{uri}'>\
                <properties/><accepts/><provides/><capabilities/><nodes/>\
            </node>"
        )

class VOSpace(unittest.TestCase):
    """
    Test VOSpace endpoints
    """
    def setUp(self):
        """
        These steps get performed before each test
        """
        self.base = f"{SERVICE_ORIGIN}/vospace-2.0/vospace"
        self.user1 = TestUser("userone")
        self.user2 = TestUser("usertwo")

    def test_availability(self):
        """
        Simple test call to discovery service to make sure it is available
        """
        res = requests.get(self.base, timeout=5000)
        self.assertEqual(res.status_code, 200)
    
    def test_get_node(self):
        pass

    def test_create_node(self):
        #TODO: this time based filename is a bit of a stopgap until we have
        # mechanisms for proper cleanup. Ideally at the  beginning of the test
        # suite we should cleanup from previous test runs
        new_dir = f"testdir-auto-{time.strftime('%Y%m%d-%H%M%S')}"
        node_xml = VosXML.container_node(self.user1.name, new_dir)
        res = requests.put(
            f"{self.base}/nodes/{self.user1.name}/{new_dir}",
            data=node_xml,
            headers={
                'Content-Type': 'application/xml',
                'X-DL-AuthToken': self.user1.token
            },
            timeout=5000
            )
        self.assertEqual(res.status_code, 201)
        self.assertIn("vos:ContainerNode", res.text)

    def test_delete_node(self):
        pass

    def test_copy_node(self):
        pass

    def test_link_node(self):
        pass

    def test_lock_node(self):
        pass

    def test_move_node(self):
        pass

    def test_set_property(self):
        pass

    def get_node_type(self):
        pass

if __name__ == "__main__":
    unittest.main()