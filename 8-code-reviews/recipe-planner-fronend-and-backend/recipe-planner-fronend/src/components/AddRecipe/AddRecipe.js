import React, {useState} from 'react';
import './AddRecipe.css';
import {Form, Button, Col, Row, Alert} from 'react-bootstrap';
import {useNavigate} from "react-router-dom";

import axios from "axios";
import AddIngredient from "../AddIngredient/AddIngredient";
import {RECIPES_URL} from "../../api";

// listId ist nur der React-Key fuer die Zeile und gehoert nicht ins Backend
const toRequestBody = (formData) => ({
    ...formData,
    ingredients: formData.ingredients.map(({listId, ...ingredient}) => ({
        ...ingredient,
        amount: Number(ingredient.amount)
    }))
})

function AddRecipe() {
    const navigate = useNavigate()
    const [formData, setFormData] = useState({
        "name": '',
        "description": '',
        "imageUrl": '',
        "ingredients": []
    })
    const [listId, setListId] = useState(1)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState(null)

    const handleChange = (field) => (e) => setFormData({...formData, [field]: e.target.value})

    const addIngredient = () => {

        setFormData(({...formData, ingredients: [
                ...formData.ingredients, {
                    listId: listId,
                    name: '',
                    unit: 'PIECE',
                    amount: ''
                }
            ]}))
        setListId(listId + 1)

    }
    const updateIngredient = (ingredientObj) => {
        const updatedIngredients = formData.ingredients.map((ingredient) => {
            if (ingredient.listId === ingredientObj.listId) {
                return ingredientObj
            }
            return ingredient
        })
        setFormData({...formData, ingredients: updatedIngredients})
    }

    const removeIngredient = (ingredientObj) => {
        const updatedIngredients = formData.ingredients.filter((ingredient) => ingredient.listId !== ingredientObj.listId)
        setFormData({...formData, ingredients: updatedIngredients})
    }

    const handleSubmit = (event) => {
        event.preventDefault()
        setSaving(true)
        setError(null)
        axios.post(RECIPES_URL, toRequestBody(formData))
            .then(() => navigate('/'))
            .catch(() => {
                setError('The recipe could not be saved. Is the backend running?')
                setSaving(false)
            })
    }

    const renderIngredients = formData.ingredients.map(ingredient => <AddIngredient
        key={ingredient.listId}
        ingredient={ingredient}
        updateIngredient={updateIngredient}
        removeIngredient={removeIngredient}
    />)

    return (
        <>
            <div className="bg">
                <Form className="m-3" onSubmit={handleSubmit}>
                    <h1 className="h3 bg-dark text-bg-primary mt-2">Add Recipe</h1>
                    {error && <Alert variant="danger">{error}</Alert>}
                    <Form.Group className="mb-1" controlId="formBasicName">
                        <Form.Label>Recipe Name:</Form.Label>
                        <Form.Control placeholder="Name" required value={formData.name} onChange={handleChange('name')}/>
                    </Form.Group><Form.Group className="mb-1" controlId="formBasicDescription">
                        <Form.Label>Description:</Form.Label>
                        <Form.Control placeholder="Description" value={formData.description} onChange={handleChange('description')}/>
                    </Form.Group><Form.Group className="mb-1 mb-5" controlId="formBasicImageUrl">
                        <Form.Label>Image URL:</Form.Label>
                        <Form.Control type="url" placeholder="URL" value={formData.imageUrl} onChange={handleChange('imageUrl')}/>
                    </Form.Group>
                    <Row>
                        <Col>Ingredient</Col>
                        <Col>Unit</Col>
                        <Col>Quantity</Col>
                        <Col xs={1}></Col>
                    </Row>
                    <hr/>
                    <Row>
                        <br></br>
                    </Row>
                    {renderIngredients}
                    <Row>
                        <br></br>
                        <Button
                            variant='warning'
                            onClick={addIngredient}
                            className="mt-1"
                            >Add Ingredient</Button>
                    </Row>
                    <Button variant="primary" type="submit" className="mb-5" disabled={saving}>
                        Submit
                    </Button>
                </Form>

            </div>

        </>
    )
}

export default AddRecipe;
