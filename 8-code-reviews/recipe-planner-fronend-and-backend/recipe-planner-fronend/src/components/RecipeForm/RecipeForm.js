import React, {useState} from 'react';
import './RecipeForm.css';
import {Alert, Button, Col, Form, Row} from 'react-bootstrap';

import AddIngredient from "../AddIngredient/AddIngredient";

// listId only identifies an ingredient row inside this form, the backend does not know it
const withListIds = (ingredients) => ingredients.map((ingredient, index) => ({...ingredient, listId: index + 1}))

const toPayload = (recipe) => ({
    ...recipe,
    ingredients: recipe.ingredients.map(({listId, ...ingredient}) => ({
        ...ingredient,
        amount: Number(ingredient.amount)
    }))
})

const RecipeForm = ({title, submitLabel, initialRecipe, onSubmit}) => {
    const [recipe, setRecipe] = useState({...initialRecipe, ingredients: withListIds(initialRecipe.ingredients ?? [])})
    const [nextListId, setNextListId] = useState(recipe.ingredients.length + 1)
    const [error, setError] = useState(null)

    const updateField = ({target}) => {
        setRecipe({...recipe, [target.name]: target.value})
    }

    const addIngredient = () => {
        setRecipe({
            ...recipe, ingredients: [
                ...recipe.ingredients, {
                    listId: nextListId,
                    name: '',
                    unit: 'PIECE',
                    amount: ''
                }
            ]
        })
        setNextListId(nextListId + 1)
    }

    const updateIngredient = (changedIngredient) => {
        const updatedIngredients = recipe.ingredients.map((ingredient) =>
            ingredient.listId === changedIngredient.listId ? changedIngredient : ingredient)
        setRecipe({...recipe, ingredients: updatedIngredients})
    }

    const removeIngredient = (removedIngredient) => {
        const updatedIngredients = recipe.ingredients.filter((ingredient) =>
            ingredient.listId !== removedIngredient.listId)
        setRecipe({...recipe, ingredients: updatedIngredients})
    }

    const submit = (event) => {
        event.preventDefault()
        setError(null)
        onSubmit(toPayload(recipe))
            .catch(() => setError("The recipe could not be saved. Is the backend running?"))
    }

    return (
        <div className="bg">
            <div className="m-3">
                <h1 className="h3 bg-dark text-bg-primary mt-2">{title}</h1>
                <Form onSubmit={submit}>
                    <Form.Group className="mb-1" controlId="formBasicName">
                        <Form.Label>Recipe Name:</Form.Label>
                        <Form.Control
                            name="name"
                            placeholder="Name"
                            value={recipe.name}
                            onChange={updateField}
                            required
                        />
                    </Form.Group>
                    <Form.Group className="mb-1" controlId="formBasicDescription">
                        <Form.Label>Description:</Form.Label>
                        <Form.Control
                            name="description"
                            placeholder="Description"
                            value={recipe.description}
                            onChange={updateField}
                        />
                    </Form.Group>
                    <Form.Group className="mb-1 mb-5" controlId="formBasicImageUrl">
                        <Form.Label>Image URL:</Form.Label>
                        <Form.Control
                            name="imageUrl"
                            type="url"
                            placeholder="URL"
                            value={recipe.imageUrl}
                            onChange={updateField}
                        />
                    </Form.Group>

                    <Row>
                        <Col>Ingredient</Col>
                        <Col>Unit</Col>
                        <Col>Quantity</Col>
                        <Col xs={1}/>
                    </Row>
                    <hr/>

                    {recipe.ingredients.map(ingredient => <AddIngredient
                        key={ingredient.listId}
                        ingredient={ingredient}
                        updateIngredient={updateIngredient}
                        removeIngredient={removeIngredient}
                    />)}

                    <Row>
                        <Button
                            variant='warning'
                            onClick={addIngredient}
                            className="mt-1"
                        >Add Ingredient</Button>
                    </Row>

                    {error && <Alert variant="danger" className="mt-3">{error}</Alert>}

                    <Button variant="primary" type="submit" className="mb-5">
                        {submitLabel}
                    </Button>
                </Form>
            </div>
        </div>
    )
}

export default RecipeForm;
