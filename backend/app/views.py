from django.shortcuts import render
from django.db import connection
from django.http import JsonResponse, HttpResponse

@csrf_exempt
def postnoteplace(request):
    if request.method != 'POST':
        return HttpResponse(status=400)

    # loading form-encoded data
    message = request.POST.get("message")
    lat = request.POST.get("lat")
    lng = request.POST.get("lng")
    x = request.POST.get("x")
    y = request.POST.get("y")
    z = request.POST.get("z")
    orientation = request.POST.get("orientation")
    if request.FILES.get("image"):
        content = request.FILES['image']
        filename = username+str(time.time())+".jpeg"
        fs = FileSystemStorage()
        filename = fs.save(filename, content)
        imageurl = fs.url(filename)
    else:
        imageurl = None
        
    cursor = connection.cursor()
    cursor.execute('INSERT INTO notes (message, lat, lng, x, y, z, orientation, imageurl) VALUES'
                   '(%s, %s, %s, %s, %s, %s, %s, %s)  RETURNING ID;', (message, lat, lng, x, y, z, orientation, imageurl))

    ID = cursor.fetchone()[0]

    return JsonResponse({"ID":ID})

@csrf_exempt
def getnote(request):
    if request.method != 'GET':
        return HttpResponse(status=400)
    
    uid = request.GET['ID']

    cursor = connection.cursor()
    cursor.execute(f"SELECT * FROM notes WHERE ID = '{uid}';")
    rows = cursor.fetchone()

    response = {}
    response['notes'] = rows
    return JsonResponse(response)