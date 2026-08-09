from fastapi import FastAPI
from pydantic import BaseModel
import numpy as np
from sklearn.linear_model import LinearRegression

app = FastAPI()

class PredictionRequest(BaseModel):
    values: list

@app.post("/predict")
def predict(data: PredictionRequest):

    values = np.array(data.values)

    # time index (1,2,3...)
    X = np.arange(1, len(values)+1).reshape(-1,1)

    model = LinearRegression()
    model.fit(X, values)

    next_month = np.array([[len(values)+1]])
    prediction = model.predict(next_month)

    return {"prediction": float(prediction[0])}