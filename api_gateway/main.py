import os
from fastapi import FastAPI, Request, HTTPException, Header
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.staticfiles import StaticFiles
from dotenv import load_dotenv
import httpx
import pathlib

load_dotenv()

app = FastAPI(title="API Gateway")

ORDER_SERVICE_URL = os.getenv("ORDER_SERVICE_URL", "http://localhost:8001")
INVENTORY_URL = os.getenv("INVENTORY_URL", "http://localhost:8002")
AUTH_URL = os.getenv("AUTH_URL", "http://localhost:8003")
CART_URL = os.getenv("CART_URL", "http://localhost:8004")
PAYMENT_URL = os.getenv("PAYMENT_URL", "http://localhost:8005")

static_dir = pathlib.Path(__file__).parent / "static"
app.mount("/static", StaticFiles(directory=static_dir), name="static")

@app.get("/", response_class=HTMLResponse)
async def read_index():
    with open(static_dir / "index.html", "r") as f:
        return f.read()

# Middleware to check token for /cart and /payment
@app.middleware("http")
async def check_auth_token(request: Request, call_next):
    path = request.url.path
    if path.startswith("/cart") or path.startswith("/payment"):
        token = request.headers.get("token")
        if not token:
            return JSONResponse(status_code=401, content={"detail": "Missing token"})
            
        async with httpx.AsyncClient() as client:
            try:
                auth_res = await client.get(f"{AUTH_URL}/auth/me", headers={"token": token})
                if auth_res.status_code != 200:
                    return JSONResponse(status_code=401, content={"detail": "Invalid token"})
                
                # Attach user_id to request state so we can add it to forwarded requests
                user_data = auth_res.json()
                request.state.user_id = user_data["user_id"]
                request.state.username = user_data["username"]
            except Exception as e:
                return JSONResponse(status_code=500, content={"detail": "Auth service unavailable"})
    
    response = await call_next(request)
    return response

# Forwarding routes
@app.api_route("/auth", methods=["GET", "POST", "PUT", "DELETE"])
@app.api_route("/auth/{full_path:path}", methods=["GET", "POST", "PUT", "DELETE"])
async def auth_routes(request: Request, full_path: str = ""):
    async with httpx.AsyncClient() as client:
        body = await request.body()
        req_headers = dict(request.headers)
        req_headers.pop("host", None)
        req_headers.pop("content-length", None)
        try:
            url = f"{AUTH_URL}/auth/{full_path}" if full_path else f"{AUTH_URL}/auth"
            res = await client.request(
                request.method,
                url,
                content=body,
                headers=req_headers,
                params=request.query_params
            )
            return JSONResponse(status_code=res.status_code, content=res.json() if res.content else None)
        except Exception as e:
            return JSONResponse(status_code=500, content={"detail": str(e)})

@app.api_route("/cart", methods=["GET", "POST", "PUT", "DELETE"])
@app.api_route("/cart/{full_path:path}", methods=["GET", "POST", "PUT", "DELETE"])
async def cart_routes(request: Request, full_path: str = ""):
    async with httpx.AsyncClient() as client:
        body = await request.body()
        req_headers = dict(request.headers)
        req_headers.pop("host", None)
        req_headers.pop("content-length", None)
        # Add user_id from state
        if hasattr(request.state, "user_id"):
            req_headers["x-user-id"] = str(request.state.user_id)
            
        try:
            url = f"{CART_URL}/cart/{full_path}" if full_path else f"{CART_URL}/cart"
            res = await client.request(
                request.method,
                url,
                content=body,
                headers=req_headers,
                params=request.query_params
            )
            return JSONResponse(status_code=res.status_code, content=res.json() if res.content else None)
        except Exception as e:
            return JSONResponse(status_code=500, content={"detail": str(e)})

@app.api_route("/payment", methods=["GET", "POST", "PUT", "DELETE"])
@app.api_route("/payment/{full_path:path}", methods=["GET", "POST", "PUT", "DELETE"])
async def payment_routes(request: Request, full_path: str = ""):
    async with httpx.AsyncClient() as client:
        body = await request.body()
        req_headers = dict(request.headers)
        req_headers.pop("host", None)
        req_headers.pop("content-length", None)
        if hasattr(request.state, "user_id"):
            req_headers["x-user-id"] = str(request.state.user_id)
            
        try:
            url = f"{PAYMENT_URL}/payment/{full_path}" if full_path else f"{PAYMENT_URL}/payment"
            res = await client.request(
                request.method,
                url,
                content=body,
                headers=req_headers,
                params=request.query_params
            )
            return JSONResponse(status_code=res.status_code, content=res.json() if res.content else None)
        except Exception as e:
            return JSONResponse(status_code=500, content={"detail": str(e)})

@app.api_route("/inventory", methods=["GET", "POST", "PUT", "DELETE"])
@app.api_route("/inventory/{full_path:path}", methods=["GET", "POST", "PUT", "DELETE"])
async def inventory_routes(request: Request, full_path: str = ""):
    async with httpx.AsyncClient() as client:
        body = await request.body()
        req_headers = dict(request.headers)
        req_headers.pop("host", None)
        req_headers.pop("content-length", None)
        try:
            url = f"{INVENTORY_URL}/inventory/{full_path}" if full_path else f"{INVENTORY_URL}/inventory"
            res = await client.request(
                request.method,
                url,
                content=body,
                headers=req_headers,
                params=request.query_params
            )
            return JSONResponse(status_code=res.status_code, content=res.json() if res.content else None)
        except Exception as e:
            return JSONResponse(status_code=500, content={"detail": str(e)})

# Keep the old routes for backward compatibility with order-service (until it's retired)
@app.post("/api/orders")
async def place_order(request: Request):
    data = await request.json()
    async with httpx.AsyncClient() as client:
        try:
            response = await client.post(f"{ORDER_SERVICE_URL}/orders", json=data)
            return JSONResponse(status_code=response.status_code, content=response.json())
        except Exception as e:
            return JSONResponse(status_code=500, content={"detail": str(e)})

@app.get("/api/inventory")
async def get_inventory_old():
    async with httpx.AsyncClient() as client:
        try:
            response = await client.get(f"{INVENTORY_URL}/inventory")
            return JSONResponse(status_code=response.status_code, content=response.json())
        except Exception as e:
            return JSONResponse(status_code=500, content={"detail": str(e)})

@app.get("/health")
def health():
    return {"status": "ok"}
