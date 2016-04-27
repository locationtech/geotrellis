class GetComponent(object):
    def get(self):
        pass

def get_component(_get):
    class TempGetComponent(GetComponent):
        def get(self):
            return _get
    return TempGetComponent()

